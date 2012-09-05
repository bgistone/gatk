package se.uu.medsci.queue.setup
import java.io.File
import collection.JavaConversions._
import java.io.FileNotFoundException


trait SampleAPI {
    def getFastqs(): ReadPairContainer    
    def getReadGroupInformation(): String   
    def getReference(): File
    def getSampleName(): String
}


class Sample(sampleName: String, setupXMLReader: SetupXMLReaderAPI, illuminaXMLReportReader: IlluminaXMLReportReaderAPI) extends SampleAPI {

    /**
     * Private variables
     */
    private val readPairContainer: ReadPairContainer = {
	    val sampleDirectory: File = setupXMLReader.getSampleFolder(sampleName)
	    
	    val fastq1: List[File] = sampleDirectory.listFiles().filter(f => f.getName().contains("_R1_")).toList
	    val fastq2: List[File] = sampleDirectory.listFiles().filter(f => f.getName().contains("_R2_")).toList  
	  
	    if(fastq1.size == 1 && fastq2.size == 1)
	    	new ReadPairContainer(fastq1.get(0), fastq2.get(0), sampleName)    
	    else if (fastq1.size == 1 && fastq2.size == 0)
	    	new ReadPairContainer(fastq1.get(0), null, sampleName)
	    else
	    	throw new FileNotFoundException("Problem with read pairs in folder: " + sampleDirectory.getAbsolutePath() + " could not find suitable files.")
    }
    
    private val readGroupInfo: String = {
        val readGroupId = illuminaXMLReportReader.getReadGroupID(sampleName)
        val sequencingCenter = setupXMLReader.getSequencingCenter()
        val readLibrary = illuminaXMLReportReader.getReadLibrary(sampleName)
        val platform = setupXMLReader.getPlatform()
        val platformUnitId = illuminaXMLReportReader.getPlatformUnitID(sampleName)
        
        parseToBwaApprovedString(readGroupId, sequencingCenter, readLibrary, platform, platformUnitId, sampleName)        
    }
    
    private val reference: File = {
        setupXMLReader.getReference(sampleName)        
    }
    
    /**
     * Public methods
     */
    
    def getSampleName(): String = {
        sampleName
    }
    
    def getFastqs(): ReadPairContainer = {
        readPairContainer    
    }
    
    def getReadGroupInformation(): String = {
        readGroupInfo
    }
    
    
    def getReference(): File = {
        reference
    }
    
    override
    def equals(that: Any): Boolean = {                
        
        that.isInstanceOf[Sample] && 
        this.sampleName.equals((that.asInstanceOf[Sample]).getSampleName())
    }
    
    override
    def hashCode(): Int = {
        sampleName.hashCode()
    }

    /**
     * Private methods
     */
    private def parseToBwaApprovedString(readGroupId: String, sequencingCenter: String, readLibrary: String,
          					   platform: String, platformUnit: String, sampleName: String): String ={
      
      // The form which bwa wants, according to their manual is: @RG\tID:foo\tSM:bar
      val readGroupHeader: String = "\"" + """@RG\tID:""" + readGroupId + """\\tSM:""" + sampleName + """\\tCN:""" + sequencingCenter + """\\tLB:""" + readLibrary + 
      """\\tPL:""" + platform + """\\tPU:""" + platformUnit + "\""     
      
      return readGroupHeader
    }
}