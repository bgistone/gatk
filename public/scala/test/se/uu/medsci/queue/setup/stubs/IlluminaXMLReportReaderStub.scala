package se.uu.medsci.queue.setup.stubs

import se.uu.medsci.queue.setup.IlluminaXMLReportReaderAPI

class IlluminaXMLReportReaderStub extends IlluminaXMLReportReaderAPI{
    
    var sampleName: String = null
    var flowCellId: String = null
    var platformUnitId: String = null
    var readGroupId: String = null
    var readLibrary: String = null
    var lanes: List[Int] = null
    
    
    def getReadLibrary(sampleName: String): String = readLibrary
    def getFlowcellId(): String = flowCellId
    def getPlatformUnitID(sampleName: String, lane:Int): String = platformUnitId
    def getReadGroupID(sampleName: String, lane: Int): String  = readGroupId
    def getLanes(sampleName: String): List[Int] = lanes

}