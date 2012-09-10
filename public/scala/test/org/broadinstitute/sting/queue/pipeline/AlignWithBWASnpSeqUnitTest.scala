package org.broadinstitute.sting.queue.pipeline

import org.testng.annotations.Test
import scala.sys.process._
import se.uu.medsci.queue.SnpSeqBaseTest

class AlignWithBWASnpSeqPipelineTest {
       
    val snpSeqBaseTest: SnpSeqBaseTest = new SnpSeqBaseTest()
    val pathToScript: String = " -S public/scala/qscript/org/broadinstitute/sting/queue/qscripts/AlignWithBWA.scala "
    
    // Using the process package to check which bwa version is loaded, and using that
    // for running bwa in the pipeline.
    val pathToBwa = "which bwa".!!.replace("\n","")
    
    val walltime = 3600    
    
    case class EnvironmentSetup(commandline: String, jobrunner: Seq[String], pathToBwa: String) {}
    
    // Get the host name and return appropriate command line
    val envSetup:EnvironmentSetup = java.net.InetAddress.getLocalHost.getHostName() match {          
      	case """.*\.uppmax\.uu\.se.*""" =>  EnvironmentSetup(pathToScript + " -jobNative -p core -n 1' --job_walltime " + walltime, Seq("Drmaa"), pathToBwa)
      	case _ => EnvironmentSetup(pathToScript, Seq("Shell"), pathToBwa)
	}       

  @Test
  def testPairedEndAlignment {
    val projectName = "test"
    val testOut = "1.bam"
    val spec = new PipelineTestSpec()
  
    spec.jobRunners = envSetup.jobrunner
    
    spec.name = "AlignPairedEndWithBwa"
    spec.args = Array(envSetup.commandline,
            		  " -bwa " + envSetup.pathToBwa,
    				  " -i " + snpSeqBaseTest.pathToBaseDir + "pipelineSetup.xml",
    				  " -bwape ",
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> "49c5c674c9fbf3dd95c7bb2d3037ff4d"
    PipelineTest.executeTest(spec)
  }
 
  @Test
  def testSingleEndAlignment {
    val projectName = "test"
    val testOut = "1.bam"
    val spec = new PipelineTestSpec()
  
    spec.jobRunners = envSetup.jobrunner
    
    spec.name = "AlignSingleEndWithBwa"
    spec.args = Array(envSetup.commandline,
            		  " -bwa " + envSetup.pathToBwa,
    				  " -i " + snpSeqBaseTest.pathToBaseDir + "pipelineSetup.xml",
    				  " -bwase ",
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> "8ca2fd93b6eb7ac5b899bd2d3b32a7f6"
    PipelineTest.executeTest(spec)
  }
 
  @Test
  def testBwaSWAlignment {
    val projectName = "test"
    val testOut = "1.bam"
    val spec = new PipelineTestSpec()
  
    spec.jobRunners = envSetup.jobrunner
    
    spec.name = "AlignSWWithBwa"
    spec.args = Array(envSetup.commandline,
            		  " -bwa " + envSetup.pathToBwa,
    				  " -i " + snpSeqBaseTest.pathToBaseDir + "pipelineSetup.xml",
    				  " -bwasw ",
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> "00a8b168ab0c242406e54f9243d60211"
    PipelineTest.executeTest(spec)
  }
  
  
  //TODO Test abort/resume functionality
  
}