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
                //TODO Fix new md5s
                val md5 = "88d073bc43b6c019653787f58628c744"                
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
            case _ => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
                val md5 = "4451f6599fce29ea2612a3a87fb2ec59"
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
	    				  " -i " + snpSeqBaseTest.pathSetupFile,
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
                //TODO Fix new md5s
                val md5 = "4f5aa4cff97c7940ca17e552cf499817"                
                Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
            }
            case _ => {
                val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
                val md5 = "8ece3ccce3b2c83dc6ebbcac98c16caf"
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
    		  			" -i " + snpSeqBaseTest.pathSetupFile,
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
    				  " -i " + snpSeqBaseTest.pathSetupFile,
    				  " -bwasw ",
    				  " -wallTime " + walltime,
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> md5sum
    PipelineTest.executeTest(spec)
  }
  
    /**
    * testSameSampleInMoreThanOneRunFolder 
    */
    
  @DataProvider(name = "testSameSampleInMoreThanOneRunFolderDataProvider")
  def SameSampleInMoreThanOneRunFolderDataProvider: Array[Array[Object]] = {        
    runOnUppmax match {
        case true => {
            val envSetup = EnvironmentSetup(pathToScript, Seq("Drmaa"), "/bubo/sw/apps/bioinfo/bwa/0.6.2/kalkyl/bwa");
            //TODO Fix new md5s
            val md5 = "8affd69d2b506bd7d35bdd226f27d057"                
            Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
        }
        case _ => {
            val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
            val md5 = "c0be0a282bd20b1300adfe55b6b3a488"
            Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
        }
    }                     							   
  }
  
  @Test(dataProvider="testSameSampleInMoreThanOneRunFolderDataProvider")
  def SameSampleInMoreThanOneRunFolder(envSetup: EnvironmentSetup, md5sum: String) {
    val projectName = "test"
    val testOut = "1.bam"
    val spec = new PipelineTestSpec()
  
    spec.jobRunners = envSetup.jobrunner
    
    spec.name = "SameSampleInMoreThanOneRunFolder"
    spec.args = Array(envSetup.commandline,
            		  " -bwa " + envSetup.pathToBwa,
    				  " -i " + snpSeqBaseTest.pathToSetupFileForSameSampleAcrossMultipleRunFolders,
    				  " -bwape ",
    				  " -wallTime " + walltime,
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> md5sum
    PipelineTest.executeTest(spec)
  } 
  
  
  /**
   * testSameSampleAcrossSeveralLanes
   */
    
  //TODO Fix this!
  @DataProvider(name = "testSameSampleAcrossSeveralLanesDataProvider")
  def testSameSampleAcrossSeveralLanesDataProvider: Array[Array[Object]] = {        
    runOnUppmax match {
        case true => {
            val envSetup = EnvironmentSetup(pathToScript, Seq("Drmaa"), "/bubo/sw/apps/bioinfo/bwa/0.6.2/kalkyl/bwa");
            //TODO Fix new md5s
            val md5 = ""                
            Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
        }
        case _ => {
            val envSetup = EnvironmentSetup(pathToScript, Seq("Shell"), "/usr/bin/bwa");
            val md5 = "9774391df9f3e9825757139699ac09f6"
            Array(Array(envSetup, md5)).asInstanceOf[Array[Array[Object]]]
        }
    }                     							   
  }
 
  //TODO Fix this!
  @Test(dataProvider="testSameSampleAcrossSeveralLanesDataProvider")
  def testSameSampleAcrossSeveralLanes(envSetup: EnvironmentSetup, md5sum: String) {
    val projectName = "test"
    val testOut = "1.bam"
    val spec = new PipelineTestSpec()
  
    spec.jobRunners = envSetup.jobrunner
    
    spec.name = "SameSampleAcrossSeveralLanes"
    spec.args = Array(envSetup.commandline,
            		  " -bwa " + envSetup.pathToBwa,
    				  " -i " + snpSeqBaseTest.pathToSetupFileForSameSampleAcrossMultipleLanes,
    				  " -bwape ",
    				  " -wallTime " + walltime,
    				  " -startFromScratch ").mkString
    spec.fileMD5s += testOut -> md5sum
    PipelineTest.executeTest(spec)
  } 
}