package se.uu.medsci.queue.setup
import java.io.File
import scala.collection.Seq
import scala.xml._
import collection.JavaConversions._


trait SetupXMLReaderAPI {
    
    def getSampleFolder(sampleName: String): File    
    def getPlatform(): String
    def getSequencingCenter(): String   
    def getProjectName(): String
    def getSamples(): Seq[SampleAPI]
    def getReference(sampleName: String): File
    def getUppmaxProjectId(): String
}

class SetupXMLReader(setupXML: File) extends SetupXMLReaderAPI{

    val xml = XML.loadFile(setupXML)
    
    
    def getSampleFolder(sampleName: String): File = {
        val sampleFolderNode = xml.\\("SampleFolder").find(node => node.attribute("Name").get.text.equalsIgnoreCase(sampleName))
        new File(sampleFolderNode.get.attribute("Path").get.text)
    }
    
    def getPlatform(): String = {
        xml.\\("Project")(0).attribute("Platform").get.text
    }
    
    def getUppmaxProjectId(): String = {
        xml.\\("Project")(0).attribute("UppmaxProjectId").get.text
    }
    
    def getSequencingCenter(): String = {
    
        xml.\\("Project")(0).attribute("SequencingCenter").get.text
        
    }
    
    def getProjectName(): String = {
        
        xml.\\("Project")(0).attribute("Name").get.text
        
    }
    
    def getSamples(): Seq[SampleAPI] = {
        // For each sample in setupXML, create a new sample instance and add it to a Seq
        var samples: Seq[SampleAPI] = Seq()
        
        val runFolderNodes = xml.\\("RunFolder")
        
        for (runFolderNode <- runFolderNodes){
        	
            val sampleNodes = xml.\\("SampleFolder")              

            for(sampleNode <- sampleNodes) {

	            val illuminaXMLReportFile: File = new File(runFolderNode.attribute("Report").get.text)
	            val illuminaXMLReportReader: IlluminaXMLReportReader = new IlluminaXMLReportReader(illuminaXMLReportFile)
	            val sampleName = sampleNode.attribute("Name").get.text
	            
	            samples :+= new Sample(sampleName, this, illuminaXMLReportReader)               
	            
	        }
        }
    	samples
    }
    
   
    def getReference(sampleName: String): File = {        
        val sampleFolderNode = xml.\\("SampleFolder").find(node => node.attribute("Name").get.text.equalsIgnoreCase(sampleName))
        new File(sampleFolderNode.get.attribute("Reference").get.text)
    }    
}