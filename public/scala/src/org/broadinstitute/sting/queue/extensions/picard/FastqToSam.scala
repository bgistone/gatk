/*
 * Copyright (c) 2012, The Broad Institute
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

package org.broadinstitute.sting.queue.extensions.picard

import org.broadinstitute.sting.commandline._
import java.io.File
import net.sf.picard.util.FastqQualityFormat
import net.sf.samtools.util.Iso8601Date
import net.sf.samtools.SAMUtils

class FastqToSam extends org.broadinstitute.sting.queue.function.JavaCommandLineFunction with PicardBamFunction {
	
	analysisName = "FastqToSam"
	javaMainClass = "net.sf.picard.sam.FastqToSam"
  
    @Input(shortName = "fastq1", fullName = "input_fastq_1", required = true, doc = "Input fastq file (optionally gzipped) for single end data, or first read in paired end data. Required.")
  	var fastq1: File = null

  	@Input(shortName = "fastq2", fullName = "input_fastq_2", required = true, doc = "Input fastq file (optionally gzipped) for the second read of paired end data. Default value: null.")
  	var fastq2: File = null
  	
  	@Argument(shortName = "qual", fullName = "quality_format", required = false, doc = "A value describing how the quality values are encoded in the fastq. Either Solexa for pre-pipeline 1.3 style scores (solexa scaling + 66), Illumina for pipeline 1.3 and above (phred scaling + 64) or Standard for phred scaled scores with a character shift of 33. Required. Possible values: {Solexa, Illumina, Standard}. Default is Standard")
  	// Illumina uses standad 33 offset since Casvava 1.8
  	var qualityFormat: FastqQualityFormat = FastqQualityFormat.Standard
  	
  	@Output(shortName = "out", fullName = "output", required = true, doc = "Output SAM/BAM file. Required.")
  	var output: File = _
  	
  	@Argument(shortName = "rg", fullName = "read_group_name", required = false, doc = "Read group name Default value: A. This option can be set to 'null' to clear the default value.")
  	var readGroupName: String = "A"
  	
  	@Argument(shortName = "s", fullName = "sample_name", required = true, doc = "Sample name to insert into the read group header Required.")
  	var sampleName: String = null
  	
  	@Argument(shortName = "l", fullName = "library_name", required = false, doc = "The library name to place into the LB attribute in the read group header Default value: null.")
  	var libraryName: String = null
  	
  	@Argument(shortName = "pu", fullName = "platform_unit", required = false, doc = "The platform unit (often run_barcode.lane) to insert into the read group header Default value: null.")
  	var platformUnit: String = null
  	
  	@Argument(shortName = "p", fullName = "platform", required = false, doc = "The platform type (e.g. illumina, solid) to insert into the read group header Default value: Illumina.")
  	var platform: String = "Illumina"
  	  
  	@Argument(shortName = "sc", fullName = "sequencing_center", required = false, doc = "The sequencing center from which the data originated Default value: SNP&SEQ Technology Platform - Uppsala University.") 
  	var sequencingCenter: String = ""
  	  
  	@Argument(shortName = "i", fullName = "Predicted_insert_size", required = false, doc = "Predicted median insert size, to insert into the read group header Default value: null.") 
  	var predictedInsertSize: Int = _
  	
  	@Argument(shortName = "c", fullName = "comment", required = false, doc = "Comment(s) to include in the merged output file's header. This option may be specified 0 or more times.")
  	var comment: String = null
  	
  	@Argument(shortName = "d", fullName = "description", required = false, doc = "Inserted into the read group header Default value: null.") 
  	var readGroupDescription: String = null
  	  
  	@Argument(shortName = "rd", fullName = "run_date", required = false, doc = "Date the run was produced, to insert into the read group header Default value: null.") 
  	var runDate: Iso8601Date = null
  	
  	// Sort order is inherited from PicardBamFunction
  	
  	@Argument(shortName = "minq", fullName = "min_q", required = false, doc = "Minimum quality allowed in the input fastq. An exception will be thrown if a quality is less than this value. Default value: 0. This option can be set to 'null' to clear the default value.") 
  	var minQ: Int = 0
  	
  	@Argument(shortName = "maxq", fullName = "max_q", required = false, doc = "Maximum quality allowed in the input fastq. An exception will be thrown if a quality is greater than this value. Default value: 93. This option can be set to 'null' to clear the default value.")
  	var maxQ: Int = SAMUtils.MAX_PHRED_SCORE;
  	
  	override def inputBams = null
  	override def outputBam = output
  	
  	override def commandLine = super.commandLine +
                             required("FASTQ=" + fastq1) +
                             required("FASTQ2=" + fastq2) +
                             optional("QUALITY_FORMAT=" + qualityFormat) +
                             //required("OUTPUT=" + output) +
                             optional("READ_GROUP_NAME=" + readGroupName) +
                             required("SAMPLE_NAME=" + sampleName) +
                             optional("LIBRARY_NAME=" + libraryName) +
                             optional("PLATFORM_UNIT=" +platformUnit) +
                             optional("PLATFORM="+platform) +
                             optional("SEQUENCING_CENTER=" + sequencingCenter) +
                             optional("PREDICTED_INSERT_SIZE=" + predictedInsertSize) +
                             optional("COMMENT=" + comment) +
                             optional("DESCRIPTION=" + readGroupDescription) +
                             optional("RUN_DATE=" + runDate)  +
                             optional("MIN_Q=" + minQ) +
                             optional("MAX_Q=" + maxQ)
   }