package se.uu.medsci.queue.setup

import org.testng.annotations._
import org.testng.Assert
import java.io.File
import se.uu.medsci.queue.SnpSeqBaseTest
import scala.collection.Seq
import se.uu.medsci.queue.setup.stubs.IlluminaXMLReportReaderStub


class SetupXMLReaderSnpSeqUnitTest {

     /*
     * Note the these tests are dependent on the report.xml file, so if that is changed the tests need to be updated.
     */
    val baseTest = new SnpSeqBaseTest()
    val setupFile: File = new File(baseTest.pathSetupFile)
    val setupXMLReader = new SetupXMLReader(setupFile)
    val sampleName = "1"     
    
	@Test
	def TestGetSampleFolder() = {        
        val expected: File = new File("public/testdata/smallTestFastqDataFolder/Sample_1")
        val actual: File = setupXMLReader.getSampleFolder(sampleName)
    	assert(actual == expected)
	}    

	@Test
	def TestGetPlatform() = {		
	    val expected: String = "Illumina"
        val actual: String = setupXMLReader.getPlatform()
    	assert(actual.equals(expected))
	}

	@Test
	def TestGetSequencingCenter() = {
	    val expected: String = "SnqSeq - Uppsala"
        val actual: String = setupXMLReader.getSequencingCenter()
    	assert(actual.equals(expected))	    
	}

	@Test
	def TestGetProjectName() = {
	    val expected: String = "TestProject"
        val actual: String = setupXMLReader.getProjectName()
    	assert(actual.equals(expected))
	}

	@Test
	def TestGetSamples() = {	    
	    val illuminaXMLReportReader: IlluminaXMLReportReaderAPI = new IlluminaXMLReportReaderStub()
	    val expected: Seq[Sample] = Seq(new Sample("1", setupXMLReader, illuminaXMLReportReader)) 
	    val actual: Seq[SampleAPI] = setupXMLReader.getSamples()	    	 
	    assert(expected.sameElements(actual))
	}

	@Test
	def TestGetReference() = {
        val expected: File = new File(baseTest.pathToReference)
        val actual: File = setupXMLReader.getReference(sampleName)                
        assert(expected == actual)
	}
	
	@Test
	def TestGetUppmaxProjectId() = {
	    val expected: String = "b2010028"
        val actual: String = setupXMLReader.getUppmaxProjectId()
    	assert(actual.equals(expected))
	    
	}

}