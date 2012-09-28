package se.uu.medsci.gatk.filters;

import net.sf.samtools.SAMRecord;
import net.sf.picard.util.QualityUtil;

import org.broadinstitute.sting.commandline.Argument;
import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
import org.broadinstitute.sting.gatk.filters.MalformedReadFilter;
import org.broadinstitute.sting.gatk.filters.ReadFilter;
import org.broadinstitute.sting.gatk.walkers.ReadFilters;

public class MinQualInIntervalFilter extends ReadFilter {

    @Argument(fullName = "filter_interval", shortName = "fi", doc = "Filter reads on the quality in the interval.", required = false)
    public String FILTER_INTERVAL = null;
    
    @Argument(fullName = "min_qual", shortName = "mq", doc = "Minimum quality of the bases in the interval. Default: 30", required = false)
    public int MIN_QUAL = 0;
	
    
	private String refLocationName;
	private int refLocationStart;
	private int refLocationEnd;
    
	@Override
	public void initialize(GenomeAnalysisEngine engine) {
		super.initialize(engine);
		
		if(FILTER_INTERVAL != null) {
			// Parse the location
			refLocationName = FILTER_INTERVAL.split(":")[0];
			refLocationStart = Integer.parseInt(FILTER_INTERVAL.split(":")[1].split("-")[0]);
			refLocationEnd = Integer.parseInt(FILTER_INTERVAL.split(":")[1].split("-")[1]);
		}
	}

	@Override
	public boolean filterOut(SAMRecord read) {
		
		if(FILTER_INTERVAL == null)
			return false;
		
		int alignmentStart = read.getAlignmentStart();
		int alignmentEnd = read.getAlignmentEnd();

		// Check that we are in the correct interval
		if(read.getReferenceName().equals(refLocationName) &&
				(alignmentStart <= refLocationEnd) &&
				(alignmentStart <= refLocationStart) &&
				(alignmentEnd >= refLocationStart)){


			// Calculate off set from alignment start
			int beginIndex = refLocationStart - alignmentStart;
			int endIndex = beginIndex + (refLocationEnd - refLocationStart) +1;

			
			byte[] baseQualities = read.getBaseQualities();			
			
			// Get the base qualities
			for(int i = beginIndex; i < endIndex; i++) {
				// if to low base quality return true
				if(baseQualities[i] < MIN_QUAL)
					return true;
			}
					
			// All base qualities were high enough, so send the read to the map function
			return false;
		}	
		
		// If the read didn't map in the interval filter it out
		return true;
	}

}
