package qscript.org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.QScript
import scala.xml._
import collection.JavaConversions._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function.ListWriterFunction
import se.uu.medsci.queue.setup._
import java.io.File
import java.io.PrintWriter

class AlignWithTophat extends QScript {

    qscript =>

    /**
     * **************************************************************************
     * Required Parameters
     * **************************************************************************
     */

    @Input(doc = "input pipeline setup xml", fullName = "input", shortName = "i", required = true)
    var input: File = _

    /**
     * **************************************************************************
     * Optional Parameters
     * **************************************************************************
     */

    @Input(doc = "The path to the binary of tophat", fullName = "path_to_tophat", shortName = "tophat", required = false)
    var tophatPath: File = _

    @Input(doc = "The path to the binary of samtools", fullName = "path_to_samtools", shortName = "samtools", required = false)
    var samtoolsPath: File = "/usr/bin/samtools"

    @Argument(doc = "Output path for the processed BAM files.", fullName = "output_directory", shortName = "outputDir", required = false)
    var outputDir: String = ""

    @Argument(doc = "Perform validation on the BAM files", fullName = "validation", shortName = "vs", required = false)
    var validation: Boolean = false

    @Argument(doc = "Number of threads tophat should use", fullName = "tophat_threads", shortName = "tt", required = false)
    var tophatThreads: Int = 1

    //TODO Add tophat specific stuff

    /**
     * **************************************************************************
     * Private variables
     * **************************************************************************
     */

    var projId: String = ""

    /**
     * Help methods
     */
    // Takes a list of processed BAM files and realign them using the BWA option requested  (bwase or bwape).
    // Returns a list of realigned BAM files.
    def performAlignment(fastqs: ReadPairContainer, readGroupInfo: String, reference: File, isMultipleAligment: Boolean = false): File = {

        val sampleDir = new File(outputDir + fastqs.sampleName)
        sampleDir.mkdirs()
        
        var alignedBamFile: File = new File(sampleDir + "/" + "find_files")

        val placeHolderFile = File.createTempFile("temporaryLogFile", ".txt")

        //tophat(fastq1: File, fastq2: File, sampleOutputDir: File, reference: File, intermediate: Boolean, jobOutputFile: File)
        add(tophat(fastqs.mate1, fastqs.mate2, sampleDir, reference, isMultipleAligment, placeHolderFile))

        // Add inprocess function to find all the bams matching accepted_hits.bam
        add(findAcceptedBams(sampleDir, alignedBamFile, placeHolderFile))

        return alignedBamFile
    }

    private def alignSingleSample(sample: SampleAPI): File = {
        val fastqs = sample.getFastqs()
        val readGroupInfo = sample.getReadGroupInformation()
        val reference = sample.getReference()

        // Check that the reference is indexed
        // TODO Check that there is a bowtie reference

        // Run the alignment
        performAlignment(fastqs, readGroupInfo, reference)
    }

    private def alignMultipleSamples(sampleName: String, sampleList: Seq[SampleAPI]): File = {

        // List of output bams for all inputs from the same sample
        var sampleSams: Seq[File] = Seq()

        // Counter for temporary sample names.
        var tempCounter: Int = 1

        for (sample <- sampleList) {
            val fastqs = sample.getFastqs()
            val readGroupInfo = sample.getReadGroupInformation()
            val reference = sample.getReference()

            // Add temporary run name
            fastqs.sampleName = sampleName + "." + tempCounter
            tempCounter = tempCounter + 1

            // Check that the reference is indexed
            // TODO Check that there is a bowtie reference

            // Run the alignment
            sampleSams :+= performAlignment(fastqs, readGroupInfo, reference, isMultipleAligment = true)
        }

        // Join and sort the sample bam files.
        val joinedBam = new File(outputDir + sampleName + ".bam")
        val joinedFilesIndex = new File(outputDir + sampleName + ".bai")
        add(joinBams(sampleSams, joinedBam, joinedFilesIndex))
        joinedBam
    }

    /**
     * The actual script
     */
    def script {

        // final output list of bam files
        var cohortList: Seq[File] = Seq()

        val setupReader: SetupXMLReader = new SetupXMLReader(input)

        val samples: Map[String, Seq[SampleAPI]] = setupReader.getSamples()
        projId = setupReader.getUppmaxProjectId()

        // TODO Set BOWTIE_INDEXES environment variable
        // It seems that this is done easiest throw the sh script running
        // the qscript

        for ((sampleName, sampleList) <- samples) {

            // One sample can be sequenced in multiple lanes. This handles that scenario.
            val bam: File =
                if (sampleList.size == 1)
                    alignSingleSample(sampleList.get(0))
                else
                    alignMultipleSamples(sampleName, sampleList)

            // Add the resulting file of the alignment to the output list
            cohortList :+= bam
        }

        // output a BAM list with all the processed files
        val cohortFile = new File(qscript.outputDir + setupReader.getProjectName() + ".cohort.list")
        add(writeList(cohortList, cohortFile))

    }

    /**
     * Case classes for running command lines
     */

    // General arguments to non-GATK tools
    trait ExternalCommonArgs extends CommandLineFunction {

        this.jobNativeArgs +:= "-p node -A " + projId
        this.memoryLimit = 24
        this.isIntermediate = false
    }

    case class writeList(inBams: Seq[File], outBamList: File) extends ListWriterFunction {
        this.inputFiles = inBams
        this.listFile = outBamList
        this.analysisName = "bamList"
        this.jobName = "bamList"
    }

    case class joinBams(inBams: Seq[File], outBam: File, index: File) extends MergeSamFiles with ExternalCommonArgs {
        this.input = inBams
        this.output = outBam
        this.outputIndex = index

        this.analysisName = "joinBams"
        this.jobName = "joinBams"
        this.isIntermediate = false
    }

    case class tophat(fastq1: File, fastq2: File, sampleOutputDir: File, reference: File, intermediate: Boolean, outputFile: File) extends CommandLineFunction with ExternalCommonArgs {

        // Sometime this should be kept, sometimes it shouldn't
        this.isIntermediate = intermediate

        @Input var file1 = fastq1
        @Input var file2 = fastq2
        @Input var dir = sampleOutputDir
        @Input var ref = reference

        @Output var stdOut = outputFile

        def commandLine = tophatPath + " -p " + tophatThreads +
            " --output-dir " + dir + " " + ref + " " + file1 + " " + " " + file2 +
            " 1> " + stdOut
    }

    case class findAcceptedBams(dirToSearchIn: File, bamFile: File, placeHolder: File) extends InProcessFunction {
        @Input var dir: File = dirToSearchIn
        @Input var ph: File = placeHolder
        @Output var output = bamFile

        def run() {
            val acceptedBamFiles = dir.listFiles().filter({_.getName().equals("accepted_hits.bam")})
            println("acceptedBamFiles " + acceptedBamFiles + " size: " + acceptedBamFiles.size)
            output = new File(dirToSearchIn + "/accepted_hits.bam")
        }
    }

}
