package se.uu.medsci.queue.setup

import org.testng.annotations.Test
import org.testng.Assert
import se.uu.medsci.queue.setup.stubs._

class IlluminaSequencingProjectSnpSeqUnitTest {

    @Test
    def testGetSamples() {
    	
        // Setup
        val setupXMLReaderStub: SetupXMLReaderStub = new SetupXMLReaderStub()
        val expected = Seq(new SampleStub("1"), new SampleStub("2"), new SampleStub("3"))
        setupXMLReaderStub.samples = expected
                      
        // Class under test
        val illuminaSequencingProject: IlluminaSequencingProject = new IlluminaSequencingProject(setupXMLReaderStub)
        
        // Run the test
        val actual = illuminaSequencingProject.getSamples()        
        assert(actual.sameElements(expected))
    }
    
    
}