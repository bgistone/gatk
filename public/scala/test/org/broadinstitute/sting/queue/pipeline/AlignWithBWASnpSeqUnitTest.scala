package org.broadinstitute.sting.queue.pipeline

import org.testng.annotations._
import se.uu.medsci.queue.SnpSeqBaseTest

/**
 * Test class for the AlignWithBwa qscript.
 * Each test will have a data provider, supplying the correct environment setup (containing the commandline, jobrunner and bwa path),
 * and correct md5 check sums for the result file. The result files will differ in the different environments due to the different
 * bwa versions used.
 * 
 */
class AlignWithBWASnpSeqPipelineTest {
    
    // If these tests are to run with the drmaa jobrunner, etc, specify -Dpipline.uppmax=true on the command line
    val runOnUppmax =  System.getProperty("pipeline.uppmax") == "true"
    
    val snpSeqBaseTest: SnpSeqBaseTest = new SnpSeqBaseTest()
    val pathToScript: String = " -S public/scala/qscript/org/broadinstitute/sting/queue/qscripts/AlignWithBWA.scala "    
    
    val walltime = 600    
    
    case class EnvironmentSetup(commandline: String, jobrunner: Seq[String], pathToBwa: String) {}    

    /**
     * testPairEndAlignment
     */
    @DataProvider(name = "testPairEndAlignmentDataProvider")
    def testPairEndAlignmentDataProvider: Array[Array[Object]] = {        
        runOnUppmax match {
            case true => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Drmaa"), "/bubo/sw/apps/bioinfo/bwa/0.6.2/kalkyl/bwa");
                val md5 = "88d073bc43b6c019653787f58628c744"                
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
            case _ => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
                val md5 = "49c5c674c9fbf3dd95c7bb2d3037ff4d"
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
        }                     							   
    }        							    
        							    
    @Test(dataProvider="testPairEndAlignmentDataProvider")
    def testPairedEndAlignment(envSetup: EnvironmentSetup, md5sum: String) = {
    	val projectName = "test"
    	val testOut = "1.bam"
	    val spec = new PipelineTestSpec()
	  
	    spec.jobRunners = envSetup.jobrunner
	    
	    spec.name = "AlignPairedEndWithBwa"
	    spec.args = Array(envSetup.commandline,
	            		  " -bwa " + envSetup.pathToBwa,
	    				  " -i " + snpSeqBaseTest.pathToBaseDir + "pipelineSetup.xml",
	    				  " -bwape ",
	    				  " -wallTime " + walltime,
	    				  " -startFromScratch ").mkString
	    spec.fileMD5s += testOut -> md5sum
	    PipelineTest.executeTest(spec)
    }
    
   /**
    * testSingleEndAlignment 
    */
    
    @DataProvider(name = "testSingleEndAlignmentDataProvider")
    def testSingleEndAlignmentDataProvider: Array[Array[Object]] = {        
        runOnUppmax match {
            case true => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Drmaa"), "/bubo/sw/apps/bioinfo/bwa/0.6.2/kalkyl/bwa");
                val md5 = "4f5aa4cff97c7940ca17e552cf499817"                
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
            case _ => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
                val md5 = "8ca2fd93b6eb7ac5b899bd2d3b32a7f6"
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
        }                     							   
    }
 
  @Test(dataProvider="testSingleEndAlignmentDataProvider")
  def testSingleEndAlignment(envSetup: EnvironmentSetup, md5sum: String) {
      val projectName = "test"
      val testOut = "1.bam"
      val spec = new PipelineTestSpec()
  
      spec.jobRunners = envSetup.jobrunner
    
      spec.name = "AlignSingleEndWithBwa"
      spec.args = Array(envSetup.commandline,
    		  			" -bwa " + envSetup.pathToBwa,
    		  			" -i " + snpSeqBaseTest.pathToBaseDir + "pipelineSetup.xml",
    		  			" -bwase ",
    		  			" -wallTime " + walltime,
    				  	" -startFromScratch ").mkString
      spec.fileMD5s += testOut -> md5sum
      PipelineTest.executeTest(spec)
  }
 
    /**
    * testBwaSWAlignment 
    */
    
    @DataProvider(name = "testBwaSWAlignmentDataProvider")
    def testBwaSWAlignmentDataProvider: Array[Array[Object]] = {        
        runOnUppmax match {
            case true => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Drmaa"), "/bubo/sw/apps/bioinfo/bwa/0.6.2/kalkyl/bwa");
                val md5 = "00a8b168ab0c242406e54f9243d60211"                
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
            case _ => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
                val md5 = "00a8b168ab0c242406e54f9243d60211"
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
        }                     							   
    }
  
  @Test(dataProvider="testBwaSWAlignmentDataProvider")
  def testBwaSWAlignment(envSetup: EnvironmentSetup, md5sum: String) {
    val projectName = "test"
    val testOut = "1.bam"
    val spec = new PipelineTestSpec()
  
    spec.jobRunners = envSetup.jobrunner
    
    spec.name = "AlignSWWithBwa"
    spec.args = Array(envSetup.commandline,
            		  " -bwa " + envSetup.pathToBwa,
    				  " -i " + snpSeqBaseTest.pathToBaseDir + "pipelineSetup.xml",
    				  " -bwasw ",
    				  " -wallTime " + walltime,
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> md5sum
    PipelineTest.executeTest(spec)
  }  
  
}