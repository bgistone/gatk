package org.broadinstitute.sting.utils.activeregion;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.samtools.util.StringUtil;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.broadinstitute.sting.utils.HasGenomeLocation;
import org.broadinstitute.sting.utils.clipping.ReadClipper;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: rpoplin
 * Date: 1/4/12
 */

public class ActiveRegion implements HasGenomeLocation, Comparable<ActiveRegion> {

    private final ArrayList<GATKSAMRecord> reads = new ArrayList<GATKSAMRecord>();
    private final GenomeLoc activeRegionLoc;
    private final GenomeLoc extendedLoc;
    private final int extension;
    private GenomeLoc fullExtentReferenceLoc = null;
    private final GenomeLocParser genomeLocParser;
    public final boolean isActive;

    public ActiveRegion( final GenomeLoc activeRegionLoc, final boolean isActive, final GenomeLocParser genomeLocParser, final int extension ) {
        this.activeRegionLoc = activeRegionLoc;
        this.isActive = isActive;
        this.genomeLocParser = genomeLocParser;
        this.extension = extension;
        extendedLoc = genomeLocParser.createGenomeLoc(activeRegionLoc.getContig(), activeRegionLoc.getStart() - extension, activeRegionLoc.getStop() + extension);
        fullExtentReferenceLoc = extendedLoc;
    }

    @Override
    public String toString() {
        return "ActiveRegion " + activeRegionLoc.toString();
    }

    // add each read to the bin and extend the reference genome activeRegionLoc if needed
    public void add( final GATKSAMRecord read ) {
        fullExtentReferenceLoc = fullExtentReferenceLoc.union( genomeLocParser.createGenomeLoc( read ) );
        reads.add( read );
    }
    
    public void hardClipToActiveRegion() {
        final ArrayList<GATKSAMRecord> clippedReads = ReadClipper.hardClipToRegion( reads, extendedLoc.getStart(), extendedLoc.getStop() );
        reads.clear();
        reads.addAll(clippedReads);
    }

    public ArrayList<GATKSAMRecord> getReads() { return reads; }

    public byte[] getActiveRegionReference( final IndexedFastaSequenceFile referenceReader ) {
        return getActiveRegionReference(referenceReader, 0);
    }

    public byte[] getActiveRegionReference( final IndexedFastaSequenceFile referenceReader, final int padding ) {
        return getReference( referenceReader, padding, extendedLoc );
    }

    public byte[] getFullReference( final IndexedFastaSequenceFile referenceReader ) {
        return getFullReference(referenceReader, 0);
    }

    public byte[] getFullReference( final IndexedFastaSequenceFile referenceReader, final int padding ) {
        return getReference( referenceReader, padding, fullExtentReferenceLoc );
    }

    private byte[] getReference( final IndexedFastaSequenceFile referenceReader, final int padding, final GenomeLoc genomeLoc ) {
        final byte[] reference =  referenceReader.getSubsequenceAt( genomeLoc.getContig(),
                Math.max(1, genomeLoc.getStart() - padding),
                Math.min(referenceReader.getSequenceDictionary().getSequence(genomeLoc.getContig()).getSequenceLength(), genomeLoc.getStop() + padding) ).getBases();
        StringUtil.toUpperCase(reference);
        return reference;
    }

    @Override
    public int compareTo( final ActiveRegion other ) {
        return this.getLocation().compareTo(other.getLocation());
    }

    @Override
    public GenomeLoc getLocation() { return activeRegionLoc; }
    public GenomeLoc getExtendedLoc() { return extendedLoc; }
    public GenomeLoc getReferenceLoc() { return fullExtentReferenceLoc; }

    public int getExtension() { return extension; }
    public int size() { return reads.size(); }
    public void clearReads() { reads.clear(); }
    public void remove( final GATKSAMRecord read ) { reads.remove( read ); }
    public void removeAll( final ArrayList<GATKSAMRecord> readsToRemove ) { reads.removeAll( readsToRemove ); }

    public boolean equalExceptReads(final ActiveRegion other) {
        if ( activeRegionLoc.compareTo(other.activeRegionLoc) != 0 ) return false;
        if ( isActive != other.isActive ) return false;
        if ( genomeLocParser != other.genomeLocParser ) return false;
        if ( extension != other.extension ) return false;
        if ( extendedLoc.compareTo(other.extendedLoc) != 0 ) return false;
        return true;
    }
}