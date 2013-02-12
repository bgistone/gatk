package org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript
import scala.xml._
import collection.JavaConversions._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function.ListWriterFunction
import se.uu.medsci.queue.setup._
import java.io.File
import java.io.PrintWriter

class AlignWithSTAR extends QScript {

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

    @Input(doc = "The path to the binary of star", fullName = "path_to_star", shortName = "star", required = false)
    var starPath: File = _

    @Input(doc = "The path to the binary of samtools", fullName = "path_to_samtools", shortName = "samtools", required = false)
    var samtoolsPath: File = "/usr/bin/samtools"

    @Argument(doc = "Output path for the processed BAM files.", fullName = "output_directory", shortName = "outputDir", required = false)
    var outputDir: String = ""

    @Argument(doc = "Number of threads to use", fullName = "threads", shortName = "nt", required = false)
    var threads: Int = 1

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

        // TODO ADD read group info 
        val alignedBamFile = new File(outputDir + fastqs.sampleName + ".bam")

        //star(fastq1: File, fastq2: File, sampleOutputDir: File, reference: File, outputFile: File, intermediate: Boolean)
        add(star(fastqs.mate1, fastqs.mate2, outputDir, reference, alignedBamFile, isMultipleAligment))

        return alignedBamFile
    }

    private def alignSingleSample(sample: SampleAPI): File = {
        val fastqs = sample.getFastqs()
        val readGroupInfo = sample.getReadGroupInformation()
        val reference = sample.getReference()

        // TODO Add check for reference index

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

            // TODO Add check for reference index

            // Add temporary run name
            fastqs.sampleName = sampleName + "." + tempCounter
            tempCounter = tempCounter + 1

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

    def sortAndIndex(alignedBam: File): String = " | " + samtoolsPath + " view -Su - | " + samtoolsPath + " sort - " + alignedBam.getAbsoluteFile().replace(".bam", "") + ";" +
    samtoolsPath + " index " + alignedBam.getAbsoluteFile()
    
    case class star(fastq1: File, fastq2: File, sampleOutputDir: File, reference: File, outputFile: File, intermediate: Boolean) extends CommandLineFunction with ExternalCommonArgs {

        // Sometime this should be kept, sometimes it shouldn't
        this.isIntermediate = intermediate

        @Input var file1 = fastq1
        @Input var file2 = fastq2
        @Input var dir = sampleOutputDir
        @Input var ref = reference

        @Output var out = outputFile
        
        val readFilesCommand = if(file1.endsWith(".gz")) " --readFilesCommand zcat " else ""
        
        def commandLine = starPath + " --genomeDir " + ref + " --readFilesIn " + file1 + " " + file2 +
            readFilesCommand + " --outStd SAM --outFileNamePrefix " + sampleOutputDir +
            " --runThreadN " + threads + sortAndIndex(out)
    }
}