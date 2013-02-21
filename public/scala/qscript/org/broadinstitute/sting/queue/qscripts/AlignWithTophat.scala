package qscript.org.broadinstitute.sting.queue.qscripts

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

    def performAlignment(sampleName: String, fastqs: Seq[ReadPairContainer], reference: File): (File, File) = {

        // All fastqs input to this function should be from the same sample
        // and should all be aligned to the same reference.
        val sampleDir = new File(outputDir + fastqs(0).sampleName)
        sampleDir.mkdirs()
        var alignedBamFile: File = new File(sampleDir + "/" + "accepted_hits.bam")

        val placeHolderFile = new File(sampleDir + "/qscript_tophap.stdout.log")

        val fastq1stMate = fastqs.map(container => container.mate1)         
        val fastq2ndMate = fastqs.map(container => container.mate2)
        
        add(tophat(fastq1stMate, fastq2ndMate, sampleDir, reference, placeHolderFile))

        return (alignedBamFile, placeHolderFile)
    }

    private def alignSample(sampleName: String, samples: Seq[SampleAPI]): (File, File) = {
        val fastqs = samples.map(_.getFastqs())
        val reference = if (samples.filterNot(p => {
            val pathToFirstReference = samples(0).getReference().getAbsolutePath()
            val currentReference = p.getReference.getAbsolutePath()
            currentReference.equals(pathToFirstReference)
            }).size == 0)
            samples(0).getReference()
        else
            throw new Exception("AlignWithTophat requires all instances of the same sample is aligned to the same reference.")

        // Run the alignment
        performAlignment(sampleName, fastqs, reference)
    }

    /**
     * The actual script
     */
    def script {

        // final output list of bam files
        var cohortList: Seq[File] = Seq()
        var placeHolderList: Seq[File] = Seq()

        val setupReader: SetupXMLReader = new SetupXMLReader(input)

        val samples: Map[String, Seq[SampleAPI]] = setupReader.getSamples()
        projId = setupReader.getUppmaxProjectId()

        for ((sampleName, sampleList) <- samples) {

            val (bam: File, placeHolder: File) = alignSample(sampleName, sampleList)

            placeHolderList :+= placeHolder
            // Add the resulting file of the alignment to the output list
            cohortList :+= bam
        }

        // output a BAM list with all the processed files
        val cohortFile = new File(qscript.outputDir + setupReader.getProjectName() + ".cohort.list")
        add(writeList(cohortList, cohortFile, placeHolderList))

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

    case class writeList(inBams: Seq[File], outBamList: File, placeHolder: Seq[File]) extends ListWriterFunction {
        
        @Input
        val ph = placeHolder
        
        this.inputFiles = inBams
        this.listFile = outBamList
        this.analysisName = "bamList"
        this.jobName = "bamList"
    }

    case class tophat(fastqs1: Seq[File], fastqs2: Seq[File], sampleOutputDir: File, reference: File, outputFile: File) extends CommandLineFunction with ExternalCommonArgs {

        // Sometime this should be kept, sometimes it shouldn't
        this.isIntermediate = false

        @Input var files1 = fastqs1
        @Input var files2 = fastqs2
        @Input var dir = sampleOutputDir
        @Input var ref = reference

        @Output var stdOut = outputFile

        // This handles if there are multiple files holding each mate.
        val files1CommaSepString = files1.mkString(",")
        val files2CommaSepString = files2.mkString(",")

        def commandLine = tophatPath + " -p " + tophatThreads +
            " --output-dir " + dir + " " + ref + " " + files1CommaSepString + " " + files2CommaSepString +
            " 1> " + stdOut
    }
}
