package org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.queue.AlignWithBwaFunction
import java.io.File

class Pipeline extends QScript {
    
	@Input(doc="input pipeline setup xml", fullName="input", shortName="i", required=true)
    var input: File = _   
            
    def script() {	    	    	    	   	    
	    val output: File = new File("alignWithBWAOutputDir/alignWithBwaQscriptOutput")
	    val cohortFile: File = new File("alignWithBWAOutputDir/cohort")
		add(alignWithBwa(input, output, cohortFile))
		add(helloNextFunction(cohortFile))
	}
          
    case class alignWithBwa(inputSetupXML: File, output: File, cohortFile: File) extends AlignWithBwaFunction {         
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
    

    case class helloNextFunction(input: File) extends InProcessFunction {   
    	@Input(doc="Input") var someInput = input
      
    	def run() {
    		println("Hello World")
    	}
    }

}