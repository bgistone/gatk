package se.uu.medsci.gatk.walkers;

import java.io.PrintStream;

import org.broadinstitute.sting.commandline.Argument;
import org.broadinstitute.sting.commandline.Output;
import org.broadinstitute.sting.gatk.contexts.ReferenceContext;
import org.broadinstitute.sting.gatk.refdata.ReadMetaDataTracker;
import org.broadinstitute.sting.gatk.walkers.ReadWalker;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;

public class GetBasesAtPosition extends ReadWalker<Integer, Integer> {

	@Argument(fullName = "intervalToGet", shortName = "itg", doc = "The interval that should be fetched. On form refName:start-end", required = true)
	private String intervalToGet;

	@Output
	private PrintStream out;	

	private String refLocationName;
	private int refLocationStart;
	private int refLocationEnd;

	@Override
	public void initialize() {
		super.initialize();

		// Parse the location
		refLocationName = intervalToGet.split(":")[0];
		refLocationStart = Integer.parseInt(intervalToGet.split(":")[1].split("-")[0]);
		refLocationEnd = Integer.parseInt(intervalToGet.split(":")[1].split("-")[1]);

	}

	@Override
	public Integer map(ReferenceContext ref, GATKSAMRecord read,
			ReadMetaDataTracker metaDataTracker) {

		int alignmentStart = read.getAlignmentStart();
		int alignmentEnd = read.getAlignmentEnd();

		// Check that we are in the correct interval
		if(read.getReferenceName().equals(refLocationName) &&
				(alignmentStart <= refLocationEnd) &&
				(alignmentStart <= refLocationStart) &&
				(alignmentEnd >= refLocationStart)){


			// Calculate off set from alignment start
			int beginIndex = refLocationStart - alignmentStart;
			int endIndex = beginIndex + (refLocationEnd - refLocationStart) + 1;

			try {
				// Get the specified substring and print it.
				String basesInInterval = read.getReadString().substring(beginIndex, endIndex);
				out.println(read.getReadName() + "\t" + basesInInterval);
			}
			catch(StringIndexOutOfBoundsException e) {
				logger.error(e.getMessage());
				logger.error("------------------------------------------------------");
				logger.error("refLocationStart: " + refLocationStart);
				logger.error("refLocationEnd: " + refLocationEnd);
				logger.error("alignmentStart: " + alignmentStart);
				logger.error("alignmentEnd: " + alignmentEnd);
				logger.error("beginIndex: " + beginIndex);
				logger.error("endIndex: " + endIndex);
				logger.error("------------------------------------------------------");
				System.exit(1);
			}
		}	

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer reduceInit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer reduce(Integer value, Integer sum) {
		// TODO Auto-generated method stub
		return null;
	}

}
