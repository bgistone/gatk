package qscript.org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript

import scala.xml._

import collection.JavaConversions._
import net.sf.samtools.SAMFileReader
import net.sf.samtools.SAMFileHeader.SortOrder

import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.util.QScriptUtils
import org.broadinstitute.sting.queue.function.ListWriterFunction
import org.broadinstitute.sting.commandline.Hidden

class AlignWithBWA extends QScript {
   qscript =>

  /****************************************************************************
  * Required Parameters
  ****************************************************************************/

  @Input(doc="input folder containing FASTQ files - or list of FASTQ folders files.", fullName="input", shortName="i", required=true)
  var input: File = _

  @Input(doc="Reference fasta file.", fullName="reference", shortName="R", required=true)
  var reference: File = _
  
  @Input(doc="Path to the report file.", fullName="report_path", shortName="r", required=true)
  var reportPath: File = _
  
  @Input(doc="UPPMAX project id", fullName="project_id", shortName="pid", required=true)
  var projId: String = _


  /****************************************************************************
  * Optional Parameters
  ****************************************************************************/
  
  @Input(doc="The path to the binary of bwa (usually BAM files have already been mapped - but if you want to remap this is the option)", fullName="path_to_bwa", shortName="bwa", required=false)
  var bwaPath: File = _

  @Input(doc="the project name determines the final output (BAM file) base name. Example NA12878 yields NA12878.processed.bam", fullName="project", shortName="p", required=false)
  var projectName: String = "project"

  @Input(doc="Output path for the processed BAM files.", fullName="output_directory", shortName="outputDir", required=false)
  var outputDir: String = ""
  
  @Input(doc="Decompose input BAM file and fully realign it using BWA and assume Single Ended reads", fullName="use_bwa_single_ended", shortName="bwase", required=false)
  var useBWAse: Boolean = false

  @Input(doc="Decompose input BAM file and fully realign it using BWA and assume Pair Ended reads", fullName="use_bwa_pair_ended", shortName="bwape", required=false)
  var useBWApe: Boolean = false

  @Input(doc="Decompose input BAM file and fully realign it using BWA SW", fullName="use_bwa_sw", shortName="bwasw", required=false)
  var useBWAsw: Boolean = false

  @Input(doc="Number of threads BWA should use", fullName="bwa_threads", shortName="bt", required=false)
  var bwaThreads: Int = 1

  @Input(doc="Perform validation on the BAM files", fullName="validation", shortName="vs", required=false)
  var validation: Boolean = false

  @Input(doc="Name of the sequencing center perfoming the sequencing. Default: SNP_SEQ_Platform", fullName="sequencing_center", shortName="sc", required=false)
  var sequencingCenter: String = "SNP_SEQ_Platform"

  @Input(doc="Sequencing platform. Default: ILLUMINA", fullName="platform", shortName="pl", required=false)
  var platform: String = "ILLUMINA"    

  @Input(doc="Sample folder prefix - the prefix of the sample name folder which needs to be removed. Default is: Sample_", fullName="sampleFolderPrefix", shortName="sp", required=false)
  var sampleFolderPrefix: String = "Sample_"
  
  /****************************************************************************
  * Helper classes and methods
  ****************************************************************************/  

  // Takes a list of processed BAM files and realign them using the BWA option requested  (bwase or bwape).
  // Returns a list of realigned BAM files.
  def performAlignment(fastqs: ReadPairContainer, readGroupInfo: String): File = {    
    
    val saiFile1 = new File(outputDir + fastqs.sampleName + ".1.sai")
    val saiFile2 = new File(outputDir + fastqs.sampleName + ".2.sai")
    val alignedSamFile = new File(outputDir + fastqs.sampleName + ".sam")
    val alignedBamFile = new File(outputDir + fastqs.sampleName + ".bam")
    
    // Align for single end reads
    if (useBWAse) {
        // Add jobs to the qgraph
        add(bwa_aln_se(fastqs.mate1, saiFile1),
            bwa_sam_se(fastqs.mate1, saiFile1, alignedSamFile, readGroupInfo))
    }
    // Align for paried end reads
    else if (useBWApe) {
        
        // Check that there is actually a mate pair in the container.
        assert(fastqs.isMatePaired())
        
        logger.debug("reference is: " + reference)
        logger.debug("fastqs.mate1 is: " + fastqs.mate1)
        logger.debug("fastqs.mate2 is: " + fastqs.mate2)
        
        // Add jobs to the qgraph
        add(bwa_aln_se(fastqs.mate1, saiFile1),
            bwa_aln_se(fastqs.mate2, saiFile2),
            bwa_sam_pe(fastqs.mate1, fastqs.mate2, saiFile1, saiFile2, alignedSamFile, readGroupInfo))
    }
    // Align for long single end reads using SW
    else if (useBWAsw) {
        // Add jobs to the qgraph
        add(bwa_sw(fastqs.mate1, alignedSamFile))
    }
  	add(sortSam(alignedSamFile, alignedBamFile, SortOrder.coordinate))	

  	return alignedBamFile    
  }

  /**
   * Check that all the files that make up bwa index exist for the reference.
   */
  private def checkReferenceIsBwaIndexed(reference: File): Unit = {
      assert(reference.exists(), "Could not find reference.")
      
      val referenceBasePath: String = reference.getAbsolutePath()           
      for(fileEnding <- Seq("amb", "ann", "bwt", "pac", "sa")) {
    	  assert(new File(referenceBasePath + "." + fileEnding).exists(), "Could not find index file with file ending: " + fileEnding)
      }
  }
  /**
   * Accepts a folder and returns a read pair container
   */
  private def getFastqs(folder: File): ReadPairContainer = {
            
      val fastq1: List[File] = folder.listFiles().filter(f => f.getName().contains("_R1_")).toList
      val fastq2: List[File] = folder.listFiles().filter(f => f.getName().contains("_R2_")).toList
      
      // The sample name is the folder name minus sampleFolderPrefix
      val sampleName: String = folder.getName().replace(sampleFolderPrefix, "")
      
      if(fastq1.size == 1 && fastq2.size == 1)
          ReadPairContainer(fastq1.get(0), fastq2.get(0), sampleName)    
      else if (fastq1.size == 1 && fastq2.size == 0)
          ReadPairContainer(fastq1.get(0), null, sampleName)
      else
          throw new Exception("Problem with read pars in folder: " + folder.getAbsolutePath() + " could not find suitable files.")            
  }
  
  /**
   * Case class to hold fastq mates
   */
  case class ReadPairContainer(mate1: File, mate2: File = null, sampleName: String = null) {
      def isMatePaired(): Boolean = {mate2 != null}
  }
  
  /**
   * Get the read group information for the sample being stored in the folder. It gets this information
   * from the accompanying report.xml file.
   */
  private def getReadGroupInfo(folder: File, report: Elem): String = {     
      
     // The sample should have the same name as the output folder minus "sampleFolderPrefix"
	 val sampleName = folder.getName().replace(sampleFolderPrefix, "")
  
     // Get the sample entry from the xml for the sample currently being worked on.
     val sampleEntry = report.\\("Sample").find(f => f.attribute("Id").get.text.equalsIgnoreCase(sampleName)).get
     
     // Get its library id
     val readLibrary = sampleEntry.\\("Read")(0).attribute("LibraryName").get.get(0).text
     
     // Get the flowcell id
     val flowCellId = report.\\("MetaData")(0).attribute("FlowCellId").get.text
     
     // Get the lane to use as platform unit      
     val platformUnit = flowCellId  + "." + sampleEntry.\\("Lane").map(n => (n \ "@Id").text).mkString(".")
  
     // As the read group ID should be a unique identifier - use the flowcell id and add the sample name.
     val readGroupId =  flowCellId + "." + sampleName
     
     // Get the String in the format that BWA needs
     parseToBwaApprovedString(readGroupId, sequencingCenter, readLibrary, platform, platformUnit, sampleName)
		 
  }
  
  private def parseToBwaApprovedString(readGroupId: String, sequencingCenter: String, readLibrary: String,
          					   platform: String, platformUnit: String, sampleName: String): String ={
      // The form which bwa wants, according to their manual is: @RG\tID:foo\tSM:bar
      val readGroupHeader: String = "\"" + """@RG\tID:""" + readGroupId + """\\tSM:""" + sampleName + """\\tCN:""" + sequencingCenter + """\\tLB:""" + readLibrary + 
      """\\tPL:""" + platform + """\\tPU:""" + platformUnit + "\""
      
      logger.debug("The read group header created for bwa was: " + readGroupHeader)
      
      return readGroupHeader
  }
  
  	/****************************************************************************
  	* Main script
    ****************************************************************************/
  
    def script() {
    // final output list of bam files
    var cohortList: Seq[File] = Seq()
        
	// List of fastq folders with samples to process 
    val fastqs: Seq[File] = if(input.isFile())
    							QScriptUtils.createSeqFromFile(input)
    						// Or if it is a directory
    						else
    							Seq(input)
	
    // Load the xml into memory to be queried later
    val report = if(reportPath.exists())
    	XML.loadFile(reportPath)
    else 
    	throw new Exception("Could not find the report.xml file at the specified location. Specify its location with -r.")
	
	// Check that the reference is indexed
    checkReferenceIsBwaIndexed(reference)
        
    // For each folder in the input list, perform alignment using bwa
    for (folder: File <- fastqs) {
        
        logger.debug("Running on folder: " + folder)
        
        // Get the fastq read files
        val sampleFastqs: ReadPairContainer = getFastqs(folder)
        
        // Get read group info
        var readGroupInfo: String = getReadGroupInfo(folder, report)
        
        // Run the alignment
        val bam: File = performAlignment(sampleFastqs, readGroupInfo)

        // Add the resulting file of the aligment to the output list
        cohortList :+= bam        
    }
        
    // output a BAM list with all the processed files
    val cohortFile = new File(qscript.outputDir + qscript.projectName + ".cohort.list")
    add(writeList(cohortList, cohortFile))
  }
   
  /****************************************************************************
  * Case classes - used by qgrapth to setup the job run order.
  ****************************************************************************/  
    
  // General arguments to non-GATK tools
  trait ExternalCommonArgs extends CommandLineFunction {
    
    this.jobNativeArgs +:=  "-p node -A " + projId          
    this.memoryLimit = 24
    this.isIntermediate = false
  }
  
  // Find suffix array coordinates of single end reads
  case class bwa_aln_se (fastq1: File, outSai: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="fastq file to be aligned") var fastq = fastq1
    @Output(doc="output sai file") var sai = outSai
    
    this.isIntermediate = true
    
    def commandLine = bwaPath + " aln -t " + bwaThreads + " -q 5 " + reference + " " + fastq + " > " + sai
    this.analysisName = "bwa_aln_se"
    this.jobName = "bwa_aln_se"
  }

  // TODO My current theory is that this is unnecessary if one wants to run on fastq files.
//  // Find suffix array coordinates of paired end reads
//  case class bwa_aln_pe (fastq1: File, outSai1: File, index: Int) extends CommandLineFunction with ExternalCommonArgs {
//    @Input(doc="mate file to be aligned") var mate = fastq1
//    @Output(doc="output sai file for 1st mating pair") var sai = outSai1
//    def commandLine = bwaPath + " aln -t " + bwaThreads + " -q 5 " + reference + " -b" + index + " " + mate + " > " + sai
//    this.analysisName = queueLogDir + outSai1 + ".bwa_aln_pe1"
//    this.jobName = queueLogDir + outSai1 + ".bwa_aln_pe1"
//  }

  // Perform alignment of single end reads
  case class bwa_sam_se (fastq: File, inSai: File, outBam: File, readGroupInfo: String) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="fastq file to be aligned") var mate1 = fastq
    @Input(doc="bwa alignment index file") var sai = inSai
    @Output(doc="output aligned bam file") var alignedBam = outBam
    
    // The output from this is a samfile, which can be removed later
    this.isIntermediate = true
    
    def commandLine = bwaPath + " samse " + reference + " " + sai + " " + mate1 + " -r " + readGroupInfo + " > " + alignedBam
    this.analysisName = "bwa_sam_se"
    this.jobName = "bwa_sam_se"
  }

  // Perform alignment of paired end reads
  case class bwa_sam_pe (fastq1: File, fastq2: File, inSai1: File, inSai2:File, outBam: File, readGroupInfo: String) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="fastq file with mate 1 to be aligned") var mate1 = fastq1
    @Input(doc="fastq file with mate 2 file to be aligned") var mate2 = fastq2
    @Input(doc="bwa alignment index file for 1st mating pair") var sai1 = inSai1
    @Input(doc="bwa alignment index file for 2nd mating pair") var sai2 = inSai2
    @Output(doc="output aligned bam file") var alignedBam = outBam

    // The output from this is a samfile, which can be removed later
    this.isIntermediate = true
    
    def commandLine = bwaPath + " sampe " + reference + " " + sai1 + " " + sai2 + " " + mate1 + " " + mate2 + " -r " + readGroupInfo + " > " + alignedBam
    this.analysisName = "bwa_sam_pe"
    this.jobName = "bwa_sam_pe"
  }

  // Perform Smith-Watherman aligment of single end reads
  case class bwa_sw (inFastQ: File, outBam: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc="fastq file to be aligned") var fq = inFastQ
    @Output(doc="output bam file") var bam = outBam
    def commandLine = bwaPath + " bwasw -t " + bwaThreads + " " + reference + " " + fq + " > " + bam
    this.analysisName = "bwasw"
    this.jobName = "bwasw"
  }
  
  case class writeList(inBams: Seq[File], outBamList: File) extends ListWriterFunction {
    this.inputFiles = inBams
    this.listFile = outBamList
    this.analysisName = "bamList"
    this.jobName = "bamList"
  }
    
  case class sortSam (inSam: File, outBam: File, sortOrderP: SortOrder) extends SortSam with ExternalCommonArgs {
    this.input :+= inSam
    this.output = outBam
    this.sortOrder = sortOrderP
    this.analysisName = "sortSam"
    this.jobName = "sortSam"
  }
}