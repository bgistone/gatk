/*
 * Copyright (c) 2011, The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.queue.qscripts.examples

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.util.QScriptUtils
import org.broadinstitute.sting.gatk.walkers.genotyper.GenotypeLikelihoodsCalculationModel

/**
 * TODO
 * - Implement variant filtration for the different files.
 * - Implement node/core optimization
 */

/**
 * Variant calling pipeline script which accepts either a bam file or a list of bamfiles.
 * This is based on the ExampleUnifiedGenotyper script provided with GATK. It has, however,
 * been extended and some of the input parameters have been changed to retain consistency 
 * with the other parts of the pipeline.
 */
class VariantCalling extends QScript {
	qscript =>

  /****************************************************************************
  * Required Parameters
  ****************************************************************************/

    @Input(doc="input BAM file - or list of BAM files", fullName="input", shortName="i", required=true)
	var input: File = _  
	  
    @Input(doc="Reference fasta file", fullName="reference", shortName="R", required=true)
	var reference: File = _ 
	
  /****************************************************************************
  * Optional Parameters
  ****************************************************************************/
	
	@Input(doc="Number of threads to use in thread enabled walkers. Default: 1", fullName="nbr_of_threads", shortName="nt", required=false)
    var nbrOfThreads: Int = 1
	
	// TODO Consider if this is really needed or not.
	@Input(doc="the project name determines the final output (vcf file) base name. Example NA12878 yields NA12878.vcf", fullName="project", shortName="p", required=false)
    var projectName: String = "project"
	
    @Input(doc="Output directory of the created vcf file(s), and accompaning eval etc.", fullName="output_directory", shortName="outputDir", required=false)
    var outputDir: String = ""
	
	@Input (doc="Type of variant calling to run: WHOLE_EXOME, WHOLE_GENOME, OTHER", fullName="variant_analysis", shortName="v", required=false)
	var variantAnalysis: String = "OTHER"
	    
    @Input (doc="Sequencing depth. Values: DEEP (>10x coverage per sample), SHALLOW (<10x per sample). Default: DEEP", fullName="sequencing_depth", shortName="sd", required=false)
	var sequenceDepth: String = "DEEP"    

	@Input (doc="Genotype likelihood model. Can have any of these values: SNP, INDEL, BOTH, POOLSNP, POOLINDEL, POOLBOTH. Default: BOTH", fullName="genotype_likelihoods_model", shortName="glm", required=false)
	var genotypeModel: GenotypeLikelihoodsCalculationModel.Model = GenotypeLikelihoodsCalculationModel.Model.BOTH   
	    
	@Input(doc="An optional file with a list of intervals to proccess.", shortName="L", required=false)
	var intervals: File = _

	@Argument(doc="A optional list of filter names.", shortName="filter", required=false)
	var filterNames: List[String] = Nil // Nil is an empty List, versus null which means a non-existent List.

	@Argument(doc="An optional list of filter expressions.", shortName="filterExpression", required=false)
	var filterExpressions: List[String] = Nil
		
    @Hidden
    @Input(doc="How many ways to scatter/gather", fullName="scatter_gather", shortName="sg", required=false)
    var nContigs: Int = -1	

  /****************************************************************************
  * Main script
  ****************************************************************************/
	
	def script() {
    		
	    val bams = QScriptUtils.createSeqFromFile(input)	  
	  
	    // By default scatter over the contigs
        if (nContigs < 0)
        	nContigs = QScriptUtils.getNumberOfContigs(bams(0))
	    
		logger.debug("Running variant calling")
	        
	    for (bam: File <- bams){
		    // VCF files generated by the pipeline    
		    val variants = swapExt(bam, ".bam", ".vcf")		   
		    val unfilteredEval 	 = swapExt(variants, ".vcf", ".eval")
		    
		    // This is the default case, in which no variant recalibration is run.
		    if(variantAnalysis == "OTHER"){		    	
		        
		        add(Genotyper(bam, variants),
		    	    evaluateVariants(variants, unfilteredEval))		           
		        
		    	val filteredVariants = swapExt(variants, ".unfiltered.vcf", ".filtered.vcf")
		        val filteredEval   	 = swapExt(filteredVariants, ".vcf", ".eval")       
		    	    
			    // Only add variant filtration to the pipeline if filters were passed in
			    if (filterNames.size > 0)
			    	add(filterVariants(variants, filteredVariants),
			    		evaluateVariants(filteredVariants, filteredEval))
			}
		    else if (variantAnalysis == "WHOLE_EXOME") {
		      //TODO Implement this  
		    }
		    else if (variantAnalysis == "WHOLE_GENOME"){
		      //TODO Implement this
		    }
	    }
	}
    
  // Override the normal swapExt metod by adding the outputDir to the file path by default if it is defined.
  override
  def swapExt(file: File, oldExtension: String, newExtension: String) = {
      if(outputDir.isEmpty())
    	  new File(file.getName.stripSuffix(oldExtension) + newExtension)
      else
          swapExt(outputDir, file, oldExtension, newExtension);
  } 


   /****************************************************************************
   * Classes (non-GATK programs)
   ****************************************************************************/
	
   // General arguments to non-GATK tools
	trait ExternalCommonArgs extends CommandLineFunction {
		this.memoryLimit = 24
		this.isIntermediate = true
	}

	// General arguments to GATK walkers
	trait CommandLineGATKArgs extends CommandLineGATK with ExternalCommonArgs {
		this.reference_sequence = reference
	}

	case class Genotyper (inBam: File, outVcf: File) extends UnifiedGenotyper with CommandLineGATKArgs {

	    this.scatterCount = nContigs
	    this.num_threads = nbrOfThreads
	    
		this.input_file :+= inBam
		this.out = outVcf
		
		// Assign the call confidence based on the depth of sequencing,
		// based on the recommended value from the GATK best practice guide.
		this.stand_call_conf = sequenceDepth match {
		  case "DEEP" => 30
		  case "SHALLOW" => 4
		  case exceptionsString =>  throw new Exception("Did not recognize sequencing_depth: " +  exceptionsString + ". Please" +
		  		" specify it as either DEEP och SHALLOW")
		} 
		
		// Sets if SNP/INDELS/BOTH etc. will be called or not.
		this.genotype_likelihoods_model = genotypeModel
		
		this.isIntermediate=false
}


	case class filterVariants (inVcf: File, outVcf: File) extends VariantFiltration with CommandLineGATKArgs {
	
	    this.scatterCount = nContigs 
	    
	    this.num_threads = nbrOfThreads
	    
		this.variant = inVcf
		this.out = outVcf
		this.filterName = filterNames
		this.filterExpression = filterExpressions
		this.isIntermediate = false
	
	}
	
	case class evaluateVariants (inVcf: File, outVcf: File) extends VariantEval with CommandLineGATKArgs {
	    
		this.eval :+= inVcf         
		this.out = outVcf
		this.isIntermediate = false         
	}
}