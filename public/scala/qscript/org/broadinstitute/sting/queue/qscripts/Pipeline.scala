package org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.queue.AlignWithBwaFunction
import java.io.File
import org.broadinstitute.sting.queue.extensions.queue.DataProcessingPipelineFunction

class Pipeline extends QScript {
    
	@Input(doc="input pipeline setup xml", fullName="input", shortName="i", required=true)
    var input: File = _   
            
    def script() {	    	    	    	   	  
	    
	    // Alignment with bwa
	    val outputAlignWithBwa: File = new File("alignWithBWAOutputDir/alignWithBwaQscriptOutput")	    
	    val cohortFile: File = new File("alignWithBWAOutputDir/cohort")
		add(alignWithBwa(input, outputAlignWithBwa, cohortFile))	
		
		// Data preprocessing
		val outputDataProcessingPipeline: File = new File("dataProcessingPipeline/dataProcessingPipelineQscriptOutput")		
		add(dataProcessingPipeline(cohortFile, outputDataProcessingPipeline))
	}
          
    case class alignWithBwa(inputSetupXML: File, output: File, cohortFile: File) extends AlignWithBwaFunction {         
        this.analysisName = "alignWithBwa"
        this.jobName = "alignWithBwa"
        
        this.input = inputSetupXML
        this.stdOut = output
        this.cohort = cohortFile
        this.useBWApe = true
        this.bwaThreads = 8
        this.run = true
        // TODO Use environment sensitive thinge here.
        this.bwaPath = "/usr/bin/bwa"
        // TODO Make sure to change this
        this.outputDir = "alignWithBWAOutputDir/"
    }

    case class dataProcessingPipeline(inputList: File, output: File) extends DataProcessingPipelineFunction {
        this.analysisName = "dataProcessingPipeline"
        this.jobName = "dataProcessingPipeline"
        
        this.input = inputList
        this.stdOut = output
        this.reference = new File("public/testdata/exampleFASTA.fasta")
        this.dbSNP = Seq(new File("public/testdata/exampleDBSNP.vcf"))               
        // TODO Make sure to change this
        this.outputDir = "dataProcessingPipeline/"
        this.run = true
    }
}