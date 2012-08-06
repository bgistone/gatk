package org.broadinstitute.sting.queue.qscripts

import java.io.IOException
import scala.collection.JavaConversions._
import scala.xml._
import org.broadinstitute.sting.commandline.Hidden
import org.broadinstitute.sting.commandline.InvalidArgumentException
import org.broadinstitute.sting.gatk.walkers.indels.IndelRealigner.ConsensusDeterminationModel
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function.{ListWriterFunction, SampleFileCreator}
import org.broadinstitute.sting.queue.util._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.utils.baq.BAQ.CalculationMode
import net.sf.samtools.SAMFileHeader.SortOrder
import net.sf.samtools.SAMFileReader
import java.util.{Queue, LinkedList}
import org.broadinstitute.sting.queue.QCommandLine
import org.broadinstitute.sting.queue.engine.QGraph

class Fastq2Bam extends QScript {
  qscript =>

  /****************************************************************************
  * Required Parameters
  ****************************************************************************/
  
  @Input(doc="Directory from which to get the FASTQ files to feed the pipeline. (This will recursively look for directories named Sample* and get the fastq files in those)", fullName="fastq_folder", shortName="f", required=true)
  var fastqFolder: File = _  
  
  /****************************************************************************
  * Optional Parameters
  ****************************************************************************/
  @Input(doc="the project name determines the final output (BAM file) base name. Example NA12878 yields NA12878.processed.bam", fullName="project", shortName="p", required=false)
  var projectName: String = "project"
  
  @Input(doc="Output directory of the created bam file(s).", fullName="output_directory", shortName="outputDir", required=false)
  var outputDir: String = ""
  
  @Input(doc="Path to the report file. Default: same as fastq folder", fullName="report_path", shortName="r", required=false)
  var reportPath: File = _
  
  /****************************************************************************
  * Global Variables
  ****************************************************************************/

  val queueLogDir: String = ".qlog/"  // Gracefully hide Queue's output

  
  /****************************************************************************
  * Main script
  ****************************************************************************/
     
  def script() {
  
    // final output list of bam files
    var cohortList: Seq[File] = Seq()
    
    if(fastqFolder == null)
      // TODO Fix better exception
      throw new Exception("Could not find the specified fastq folder.")
    
    if(fastqFolder.isDirectory()){
      
      //If no path has been specified, look for the report file in the fastq dir     
      if(reportPath == null)
    	  reportPath = fastqFolder.listFiles().find(f => f.getName().equals("report.xml")).get          	  
      logger.debug(reportPath)
    
      // Load the xml into memory to be queried later
      val report = if(reportPath.exists())
        XML.loadFile(reportPath)
      else 
        throw new Exception("Could not find the report.xml file at the specified location. Specify its location with -r or, add it the same folder as the fastq root folder.")
      
      // For each folder in directory matching Sample*
      for (sampleDir <- fastqFolder.listFiles().filter(f => f.getName().startsWith("Sample"))){
    	         
         // Get the first and second mate  
         val fastq1: Array[File] = sampleDir.listFiles().filter(f => f.getName().contains("_R1_"))
         val fastq2: Array[File] = sampleDir.listFiles().filter(f => f.getName().contains("_R2_"))             

         // If there is more than one file in the array, throw an exception prompting the user to
         // concatenate the files.
         if(fastq1.size > 1 || fastq2.size > 1)
           throw new IOException("More than one fastq file was found per read. Please concatenate the files so that there is one file for each mate.");
           
         // Setup variable for the convertToSam function
         val sampleName = fastq1(0).getName().split("_")(0)
         val outBam = new File(outputDir + "/" + sampleName + ".bam")
         
         // Get the read group information for the sample from the report xml.
         logger.debug("Sample Name is: " + sampleName)
         
         // Get the sample entry from the xml for the sample currently being worked on.
         val sampleEntry = report.\\("Sample").find(f => f.attribute("Id").get.text.equals(sampleName)).get
         
         // Get its library id
         val readLibrary = sampleEntry.\\("Read")(0).attribute("LibraryName").get.get(0).text //.foreach(f => f.attribute("LibraryName").get.text)
         logger.debug("Library is: " + readLibrary)
         
         // Get the lane to use as platform unit
         // TODO This might not always be available confer with Olof to see if we need another solution.
         // This is build up from the FlowCellId and the lanes that it was run on separated by "."
         val platformUnit = report.\\("MetaData")(0).attribute("FlowCellId").get.text +
         "." +
         sampleEntry.\\("Lane").map(n => (n \ "@Id").text).mkString(".")     
         
         // Add them as input to the convertToSam function
         add(convertToSam(fastq1(0),fastq2(0),outBam,sampleName, readLibrary, platformUnit))
         
         cohortList :+= outBam
      }
    }
    else
      // TODO Fix better exception
      throw new Exception("The specified fastq folder is not a directory or could not be found.")
    
    val cohortFile = new File(outputDir + "/" + projectName + ".cohort.list")
    add(writeList(cohortList, cohortFile))
  }

  /****************************************************************************
  * Classes
  ****************************************************************************/

  trait ExternalCommonArgs extends CommandLineFunction {
    this.memoryLimit = 4
    this.isIntermediate = false
  }

  trait SAMargs extends PicardBamFunction with ExternalCommonArgs {
      this.maxRecordsInRam = 100000
  }
  
  case class convertToSam (fastq_1: File, fastq_2: File, outBam: File, sample: String, library: String, platformU: String) extends FastqToSam with ExternalCommonArgs{    
    
    this.fastq1 = fastq_1
    this.fastq2 = fastq_2
    this.output = outBam
    this.sampleName = sample

    this.sequencingCenter = "SNP&SEQ Technology Platform - Uppsala University"
    this.libraryName = library
    this.platformUnit = platformU
      
    
    this.analysisName = queueLogDir + outBam + "convert_to_sam"
    this.jobName = queueLogDir + outBam + ".convert_to_sam"
  }
  
  case class writeList(inBams: Seq[File], outBamList: File) extends ListWriterFunction {
    this.inputFiles = inBams
    this.listFile = outBamList
    this.analysisName = queueLogDir + outBamList + ".bamList"
    this.jobName = queueLogDir + outBamList + ".bamList"
  }
}