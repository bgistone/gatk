package org.broadinstitute.sting.queue.extensions.queue

import org.broadinstitute.sting.queue.function.JavaCommandLineFunction
import java.io.File
import org.broadinstitute.sting.commandline._

class AlignWithBwaFunction extends QueueScriptFunction{

    this.analysisName = "AlignWithBwa"
    //TODO This probably needs to be setup in some other way - possibly with the exact path to the script 
    this.script = new File("public/scala/qscript/org/broadinstitute/sting/queue/qscripts/AlignWithBWA.scala")        
    
	/****************************************************************************
	* Required Parameters
  	****************************************************************************/

	@Input(doc="input pipeline setup xml", fullName="input", shortName="i", required=true)
  	var input: File = _
  	
  
  	/****************************************************************************
  	* Optional Parameters  	
  	****************************************************************************/  
	
	@Output(doc="Cohort file", shortName = "cf", fullName = "cohort_file", required = false)
	var cohort: File = _
  
  	@Input(doc="The path to the binary of bwa (usually BAM files have already been mapped - but if you want to remap this is the option)", fullName="path_to_bwa", shortName="bwa", required=false)
  	var bwaPath: File = _

  	@Argument(doc="Output path for the processed BAM files.", fullName="output_directory", shortName="outputDir", required=false)
  	var outputDir: String = ""
  
  	@Argument(doc="Decompose input BAM file and fully realign it using BWA and assume Single Ended reads", fullName="use_bwa_single_ended", shortName="bwase", required=false)
  	var useBWAse: Boolean = false

  	@Argument(doc="Decompose input BAM file and fully realign it using BWA and assume Pair Ended reads", fullName="use_bwa_pair_ended", shortName="bwape", required=false)
  	var useBWApe: Boolean = false

  	@Argument(doc="Decompose input BAM file and fully realign it using BWA SW", fullName="use_bwa_sw", shortName="bwasw", required=false)
  	var useBWAsw: Boolean = false

  	@Argument(doc="Number of threads BWA should use", fullName="bwa_threads", shortName="bt", required=false)
  	var bwaThreads: Int = 1

  	@Argument(doc="Perform validation on the BAM files", fullName="validation", shortName="vs", required=false)
  	var validation: Boolean = false
    

    // Setup the commandline for the script
    override def commandLine = super.commandLine +
  								required("-i", input) +
  								optional("--cohort_file", cohort) +
  								optional("-bwa", bwaPath) +
  								optional("-outputDir", outputDir, "", true, false) +
  								conditional(useBWAse, "--use_bwa_single_ended") +
  								conditional(useBWApe, "--use_bwa_pair_ended") +
  								conditional(useBWAsw, "--use_bwa_sw") +
  								optional("--bwa_threads", bwaThreads)+
  								conditional(validation, "--validation")
}