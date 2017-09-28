package de.sybig.uniprotFetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignedSequence {

    private final Logger logger = LoggerFactory.getLogger(AlignedSequence.class);

    private String type = "modified";
    private List<SequenceFeature> features;
    private String sequence;
    private int _movedStart = 0;

    private transient Isoform parentIsoform;

    public AlignedSequence(String sequence, Isoform parentIsoform) {
        SequenceFeature sf = new SequenceFeature();
        this.sequence = sequence;
        this.parentIsoform = parentIsoform;
        sf.setStart(1);
        sf.setEnd(sequence.length());
        addFeature(sf);
    }

    /**
     * Applies the modification of a isoform to this sequence.
     *
     * @param m The modification tho apply.
     * @param isoform The isoform with the modification.
     */
    public void applyModification(Modification m, Isoform isoform) {
        
        if ((m.getEnd() - m.getBegin()) > m.getSubstitution().length()) {
            logger.trace("Treating modification as deletion");
            applyDeletion(m, isoform);
        } else {
            logger.trace("Treating modification as insertion");
            applyInsertion(m, isoform);
        }

    }

    private void applyDeletion(Modification modiToApply, Isoform isoform) {
        if (!getId().equals(isoform.getId())) {
            return;
        }
        int movedStart = 0;
        for (SequenceFeature sf : features) {
            if ("gap".equals(sf.getType()) && sf.getStart() < modiToApply.getBegin()) {
                movedStart = movedStart + sf.getEnd() - sf.getStart();
            }
        }

        if (modiToApply.getSubstitution() != null && modiToApply.getSubstitution().length() > 0) {
            SequenceFeature substitution = new SequenceFeature();
            substitution.setStart(modiToApply.getBegin() + movedStart);
            substitution.setEnd(modiToApply.getBegin() + movedStart + modiToApply.getSubstitution().length()-1);
            substitution.setType("mismatch");
            addFeature(substitution);
        }
        int sub = modiToApply.getSubstitution() == null ? 0 : modiToApply.getSubstitution().length();
        if (modiToApply.getBegin() + sub - 1 > sequence.length()) {
            return;
            //TODO add test
        }
        SequenceFeature gap = new SequenceFeature();
        gap.setStart(modiToApply.getBegin() + movedStart + sub);
        gap.setEnd(modiToApply.getEnd() + movedStart);
        gap.setType("gapD");
        sequence = sequence.substring(0, (modiToApply.getBegin() + sub))
                + String.join("", Collections.nCopies(gap.getLength(), "-"))
                + sequence.substring(gap.getStart() - movedStart, sequence.length());
        this._movedStart += gap.getLength();
        addFeature(gap);
    }

    private void applyInsertion(Modification modToApply, Isoform isoform) {

        int movedStart = 0;
        for (SequenceFeature sf : features) {
            if (sf.getType()!= null && sf.getType().startsWith("gap") && sf.getStart() < modToApply.getBegin()) {
                movedStart = movedStart + sf.getEnd() - sf.getStart();
            }
        }
        logger.trace("moved start is: {}", movedStart);

        if (checkForSameModificationInCanocicalSequence(modToApply, isoform, movedStart)) {
            return;
        }

        checkForEndOverlapsInThisSequence(modToApply, isoform, movedStart);
//        /// overlap in modified sequences
        logger.trace("{} ---- {}", isoform.getId(), getId());
        if (isoform.getId().equals(getId())) {
            logger.trace("working on own sequence");
            int sub = modToApply.getSubstitution() == null ? 0 : modToApply.getSubstitution().length();
            if (sub <= (modToApply.getSubstitution().length())) {
                logger.trace("Substitution is smaller or equal to the length of the  modification, inserting mismatch in own sequence");
                // mismatch of the same length
                SequenceFeature mismatch = new SequenceFeature();
                mismatch.setStart(modToApply.getBegin() + movedStart);
                mismatch.setEnd(modToApply.getBegin() + movedStart + modToApply.getSubstitution().length()-1);;
                mismatch.setType("mismatch");
                mismatch.setMovedStart(movedStart);
                addFeature(mismatch);
            }
            logger.trace("stopping on this sequence, its the owning one.");
            return;
        }
        Range[] ranges = overlappingModiInThisSequence(modToApply);

        if (ranges != null) {
            if (ranges[0].isValid()) {
                SequenceFeature gap = new SequenceFeature();
                gap.setStart(movedStart + ranges[0].getBegin());
                gap.setEnd(movedStart + ranges[0].getEnd());
                gap.setType("gap");
                addFeature(gap);

                sequence = sequence.substring(0, (ranges[0].getBegin() - 1))
                        + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart() - 1, "-"))
                        + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());
                _movedStart += gap.getLength();
                
                movedStart += ranges[0].getLength();
                if (ranges[1].isValid()) {
                    SequenceFeature mismatch = new SequenceFeature();
                    mismatch.setStart(movedStart + ranges[1].getBegin());
                    mismatch.setEnd(movedStart + ranges[1].getEnd());
                    mismatch.setType("mismatch");
                    addFeature(mismatch);
                }
            }
        }
        if (sameModiInThisSequence(modToApply)) {
            return;
        }
        //System.out.println("inserting gap in " + modToApply);
        logger.trace("Working on the other sequence");
        int sub = modToApply.getSubstitution() == null ? 0 : modToApply.getSubstitution().length();
        if (sub > (modToApply.getLength())) {
            SequenceFeature gap = new SequenceFeature();
            int modToApplyEnd = modToApply.getEnd() + movedStart + sub - 1;
            gap.setStart(modToApply.getBegin() + movedStart);
            gap.setEnd(modToApplyEnd);
            gap.setType("gap");
            if (!featureAlreadyAdded(gap)) {
            addFeature(gap);
            logger.debug("Adding feature {} to sequence {}", gap, getId());
            
                sequence = sequence.substring(0, (modToApply.getBegin() - 1))
                        + String.join("", Collections.nCopies(gap.getLength(), "-"))
                        + sequence.substring(gap.getStart() - movedStart - 1, sequence.length());
                this._movedStart += gap.getLength();
            }

        }
    }

    private boolean sameModiInThisSequence(Modification modToApply) {
        if (parentIsoform.getModifications() == null) {
            return false;
        }
        for (Modification m : parentIsoform.getModifications()) {
            if (m.getId().equals(modToApply.getId())) {
                return true;
            }
            if (m.getBegin() == modToApply.getBegin()
                    && m.getEnd() == modToApply.getEnd()
                    && m.getSubstitution().equals(modToApply.getSubstitution())) {
                return true;
            }
        }

        return false;
    }

    private Range[] overlappingModiInThisSequence(Modification modToApply) {

        if (parentIsoform.getModifications() == null) {
            return null;
        }
        Range[] ranges = new Range[3];
        for (Modification m : parentIsoform.getModifications()) {
            if (m.getId().equals(modToApply.getId())) {
                // It is the exact same modification
                // In this case we should not get here ...
                return null;
            }
            ranges[0] = getLeftOverlap(m, modToApply);
            ranges[1] = getOverlappingRegion(m, modToApply);
            ranges[2] = getRightOverlap(m, modToApply);
        }
        return ranges;
    }

    private Range getOverlappingRegion(Modification modi1, Modification modi2) {
        Range range = new Range();
        range.setBegin(modi1.getBegin() >= modi2.getBegin() ? modi2.getBegin() : modi1.getBegin());
        range.setEnd(modi1.getEnd() <= modi2.getEnd() ? modi1.getEnd() : modi2.getEnd());
        return range;
    }

    private Range getLeftOverlap(Modification modi1, Modification modi2) {
        Range range = new Range();
        if (modi2.getBegin() < modi1.getBegin() && modi2.getEnd() > modi2.getBegin()) {

            range.setBegin(modi2.getBegin());
            range.setEnd(modi1.getBegin());
            return range;
        }
        return range;
    }

    private Range getRightOverlap(Modification modi1, Modification modi2) {
        Range range = new Range();
        if (modi2.getEnd() > modi1.getEnd() && modi2.getBegin() < modi1.getEnd()) {
            range.setBegin(modi1.getEnd());
            range.setEnd(modi2.getEnd());
            return range;
        }
        return range;
    }

    private Range getNonOverlappingBegin() {
        return null;
    }

    private Range getNonOverlappingEnd(Range range, Modification modi1, Modification modi2) {
        Range overlap = new Range();
        overlap.setBegin(range.getEnd());
        overlap.setEnd(modi1.getEnd() > modi2.getEnd() ? modi1.getEnd() : modi2.getEnd());
        return overlap;
    }

    /**
     * Insert a mismatch feature into the owning sequence. If the substitution
     * is longer than the replaced sequence, gaps are inserted into the other
     * sequences in a different step. If a mismatch was inserted, the function
     * returns <code>true</code> and nothing else has to be done with this
     * modification and this sequence.
     *
     * @param modToApply The modification to apply.
     * @param isoform The sequence of this modification.
     * @param movedStart The difference to the original start, due to previous
     * inserted gaps.
     * @return <code>true</code> if the mismatch was inserted,
     * <code>false</code> otherwise.
     */
    private boolean insertMismatchInOwningSequence(Modification modToApply, Isoform isoform, int movedStart) {
        if (true) {
            return false;
        }
        logger.trace("Inserting mismatch in owning sequence?");
        if (getId().equals(isoform.getId())) {
            logger.trace("Yes, on owning sequence. Testing substituion {}", modToApply.getSubstitution());
            if (modToApply.getSubstitution() != null) {
                logger.trace("{} <= {} - {} ?", modToApply.getSubstitution().length(), modToApply.getEnd(), modToApply.getBegin());
            }
            if (modToApply.getSubstitution() != null
                    && modToApply.getSubstitution().length() <= (modToApply.getEnd() - modToApply.getBegin())) {

                SequenceFeature mismatch = new SequenceFeature();
                mismatch.setStart(modToApply.getBegin() + movedStart);
                mismatch.setEnd(modToApply.getBegin() + modToApply.getSubstitution().length() + movedStart);
                mismatch.setType("mismatch");
                addFeature(mismatch);
                logger.trace("Mismatch inserted into owning sequence");
            }
            return true;
        }
        logger.trace(("Not on the owning sequence"));
        return false;
    }

    private boolean checkForSameModificationInCanocicalSequence(Modification modToApply, Isoform isoform, int movedStart) {
        for (SequenceFeature sf : features) {  //TODO consider moved start

            if (sf.getType() != null && sf.getType().startsWith("gap")
                    && (modToApply.getBegin() >= (sf.getStart()))
                    && ((modToApply.getBegin() + modToApply.getSubstitution().length()) <= (sf.getEnd()))) {
                return true;
            }
            // ToDo overalap at start
            //ToDo overlap at end
        }
        return false;
    }

    @Deprecated
    private boolean checkForSameModificationInThisSequence(Modification modToApply, Isoform isoform, int movedStart) {
        String subToApply = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
        if (parentIsoform.getModifications() == null) {
            return false;
        }
        for (Modification ownModification : parentIsoform.getModifications()) {
            if (modToApply.getBegin() == ownModification.getBegin()
                    && modToApply.getEnd() == ownModification.getEnd()
                    && subToApply.equals(ownModification.getSubstitution())) {
                logger.trace("{} has the same modification {} as {}", getId(), modToApply, isoform.getId());
                return true;
            }
        }

        return false;
    }

    //TODO rename / refactor. The method inserts also a featuter
    private boolean checkForEndOverlapsInThisSequence(Modification modToApply, Isoform isoform, int movedStart) {
        String subToApply = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
        if (parentIsoform.getModifications() == null) {
            return false;
        }
        for (Modification ownModification : parentIsoform.getModifications()) {

            String modSub = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
            int modRealEnd = modToApply.getBegin() + modSub.length() > modToApply.getEnd() ? modToApply.getBegin() + modSub.length() : modToApply.getEnd();
            String ownSub = ownModification.getSubstitution() != null ? ownModification.getSubstitution() : "";
            int ownRealEnd = ownModification.getBegin() + ownSub.length() > ownModification.getEnd() ? ownModification.getBegin() + ownSub.length() : ownModification.getEnd();

//             System.out.println(isoform.getId()+ "  " + modRealEnd + " -<- " + ownRealEnd + " " + this.getId());
            if (modToApply.getBegin() <= ownModification.getEnd()
                    && modRealEnd > ownRealEnd) {

                SequenceFeature gap = new SequenceFeature();
                gap.setStart(modToApply.getBegin() + ownSub.length());
                gap.setEnd(modRealEnd);
                gap.setType("gap");
                addFeature(gap);
                System.out.println("found overlap " + gap + " for " + getId());
                return true;

            }
        }
        return false;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        if (parentIsoform == null) {
            return null;
        }
        return parentIsoform.getId();
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public List<SequenceFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<SequenceFeature> features) {
        this.features = features;
    }

    private final void addFeature(SequenceFeature feature) {
        if (features == null) {
            features = new ArrayList<>();
        }
        features.add(feature);
    }

    private boolean featureAlreadyAdded(SequenceFeature gap) {
        if (features == null) {
            return false;
        }
        if (features.contains(gap)) {
            return true;
        }
        return false;
    }

}
