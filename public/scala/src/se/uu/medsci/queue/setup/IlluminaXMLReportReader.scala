package se.uu.medsci.queue.setup
import org.apache.commons.lang.NotImplementedException
import java.io.File
import collection.JavaConversions._
import scala.xml.XML

// Trait, used as interface to be able to stub the class for
// testing purposes
trait IlluminaXMLReportReaderAPI {
    def getReadLibrary(sampleName: String): String
    def getFlowcellId(): String
    def getPlatformUnitID(sampleName: String): String
    def getReadGroupID(sampleName: String): String    
}


class IlluminaXMLReportReader(report: File) extends IlluminaXMLReportReaderAPI {
    
    val xml = XML.loadFile(report)
    
    def getReadLibrary(sampleName: String): String = {
         getSampleEntry(sampleName).\\("Read")(0).attribute("LibraryName").get.get(0).text         
     }
    def getFlowcellId(): String = {
        xml.\\("MetaData")(0).attribute("FlowCellId").get.text        
    }
    def getPlatformUnitID(sampleName: String): String = {
        getFlowcellId()  + "." + getSampleEntry(sampleName).\\("Lane").map(n => (n \ "@Id").text).mkString(".")
    }
    
    def getReadGroupID(sampleName: String): String = {
         getFlowcellId() + "." + sampleName
    }
    
    private def getSampleEntry(sampleName: String) = {
        xml.\\("Sample").find(f => f.attribute("Id").get.text.equalsIgnoreCase(sampleName)).get
    }
}