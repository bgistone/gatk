package org.broadinstitute.sting.queue.extensions.queue

import org.broadinstitute.sting.commandline._
import java.io.File

class DataProcessingPipelineFunction extends QueueScriptFunction{

    
    this.analysisName = "DataProcessingPipeline"
    //TODO This probably needs to be setup in some other way - possibly with the exact path to the script 
    this.script = new File("public/scala/qscript/org/broadinstitute/sting/queue/qscripts/DataProcessingPipeline.scala")  
    
	/****************************************************************************
	* Required Parameters
	****************************************************************************/
	
	@Input(doc="input BAM file - or list of BAM files", fullName="input", shortName="i", required=true)
	var input: File = _
	
	@Input(doc="Reference fasta file", fullName="reference", shortName="R", required=true)
	var reference: File = _
	
	@Input(doc="dbsnp ROD to use (must be in VCF format)", fullName="dbsnp", shortName="D", required=true)
	var dbSNP: Seq[File] = Seq()

  	/****************************************************************************
 	* Optional Parameters
	****************************************************************************/
	
	@Input(doc="extra VCF files to use as reference indels for Indel Realignment", fullName="extra_indels", shortName="indels", required=false)
	var indels: Seq[File] = Seq()
	
	@Input(doc="The path to the binary of bwa (usually BAM files have already been mapped - but if you want to remap this is the option)", fullName="path_to_bwa", shortName="bwa", required=false)
	var bwaPath: File = _
	
	@Argument(doc="the project name determines the final output (BAM file) base name. Example NA12878 yields NA12878.processed.bam", fullName="project", shortName="p", required=false)
	var projectName: String = "project"
	
	@Argument(doc="Output path for the processed BAM files.", fullName="output_directory", shortName="outputDir", required=false)
	var outputDir: String = ""
	
	@Argument(doc="the -L interval string to be used by GATK - output bams at interval only", fullName="gatk_interval_string", shortName="L", required=false)
	var intervalString: String = ""
	
	@Input(doc="an intervals file to be used by GATK - output bams at intervals only", fullName="gatk_interval_file", shortName="intervals", required=false)
	var intervals: File = _
	
	@Argument(doc="Cleaning model: KNOWNS_ONLY, USE_READS or USE_SW", fullName="clean_model", shortName="cm", required=false)
	var cleaningModel: String = "USE_READS"
	
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
	
	@Argument(doc="Number of threads to use in thread enabled walkers. Default: 1", fullName="nbr_of_threads", shortName="nt", required=false)
	var nbrOfThreads: Int = 1
	
	
	/****************************************************************************
	* Hidden Parameters
	****************************************************************************/
	@Hidden
	@Argument(doc="How many ways to scatter/gather", fullName="scatter_gather", shortName="sg", required=false)
	var nContigs: Int = -1
	
	@Hidden
	@Argument(doc="Define the default platform for Count Covariates -- useful for techdev purposes only.", fullName="default_platform", shortName="dp", required=false)
	var defaultPlatform: String = ""
	
	@Hidden
	@Argument(doc="Run the pipeline in test mode only", fullName = "test_mode", shortName = "test", required=false)
	var testMode: Boolean = false
	
	
	// Setup the commandline for the script
    override def commandLine = super.commandLine +
  								required("-i", input) +
  								required("-R", reference) +
  								repeat("--dbsnp", dbSNP) +
  								optional("-outputDir", outputDir, "", true, false)
    
}