package se.uu.medsci.queue

class SnpSeqBaseTest {
    
    val pathToBaseDir: String = "public/testdata/"
    
    val pathToSampleFolder: String = pathToBaseDir + "smallTestFastqDataFolder/Sample_1"    
    
    val pathToMate1: String = pathToBaseDir + "smallTestFastqDataFolder/Sample_1/exampleFASTQ_R1_file.fastq"
    val pathToMate2: String = pathToBaseDir + "smallTestFastqDataFolder/Sample_1/exampleFASTQ_R2_file.fastq"
    
    val pathToReference: String = pathToBaseDir + "exampleFASTA.fasta"
    
    val pathSetupFile: String = pathToBaseDir + "pipelineSetup.xml"
    val pathToReportXML: String = pathToBaseDir + "smallTestFastqDataFolder/report.xml"

}