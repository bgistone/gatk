package org.broadinstitute.sting.queue.qscripts

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.queue.AlignWithBwaFunction
import java.io.File

class Pipeline extends QScript {
    
	@Input(doc="input pipeline setup xml", fullName="input", shortName="i", required=true)
    var input: File = _   
    
	def script() {	    	    	    
	    val cohortList: File = new File("cohort.list")
		add(alignWithBwa(input, cohortList))
	}
     
     
     case class alignWithBwa(inputSetupXML: File, cohortList: File) extends AlignWithBwaFunction {         
         this.input = inputSetupXML
         this.cohortListFile = cohortList
         this.useBWApe = true
         this.run = true
         this.bwaPath = "/usr/bin/bwa"
     }

}