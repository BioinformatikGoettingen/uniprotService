package de.sybig.uniprotFetcher;

import de.sybig.uniprotFetcher.Isoforms.Isoform;
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

    private Logger logger = LoggerFactory.getLogger(AlignedSequence.class);

    private String type = "modified";
    private List<SequenceFeature> features;
//    private String id;
    private String sequence;

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
    public void applyModification(Isoforms.Modification m, Isoform isoform) {
        if ((m.getEnd() - m.getBegin()) > m.getSubstitution().length()) {
            applyDeletion(m, isoform);
        } else {
            applyInsertion(m, isoform);
        }

    }

    private void applyDeletion(Isoforms.Modification m, Isoform isoform) {
        if (getId().equals(isoform.getId())) {
            return;
        }
        int movedStart = 0;
        for (SequenceFeature sf : features) {
            if ("gap".equals(sf.getType()) && sf.getStart() < m.getBegin()) {
                movedStart = movedStart + sf.getEnd() - sf.getStart();
            }
        }

        if (m.getSubstitution() != null && m.getSubstitution().length() > 0) {
            SequenceFeature substitution = new SequenceFeature();
            substitution.setStart(m.getBegin() + movedStart);
            substitution.setEnd(m.getBegin() + movedStart + m.getSubstitution().length());
            substitution.setType("mismatch");
            addFeature(substitution);
        }
        int sub = m.getSubstitution() == null ? 0 : m.getSubstitution().length();
        if (m.getBegin() + sub - 1 >sequence.length()){
            return;
            //TODO add test
        }
        SequenceFeature gap = new SequenceFeature();
        gap.setStart(m.getBegin() + movedStart + sub);
        gap.setEnd(m.getEnd() + movedStart);
        gap.setType("gapD");
        sequence = sequence.substring(0, (m.getBegin() + sub - 1))
                + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart(), "-"))
                + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());
        addFeature(gap);
    }

    private void applyInsertion(Isoforms.Modification modToApply, Isoform isoform) {

        int movedStart = 0;
        for (SequenceFeature sf : features) {
            if ("gap".equals(sf.getType()) && sf.getStart() < modToApply.getBegin()) {
                movedStart = movedStart + sf.getEnd() - sf.getStart();
            }
        }

        if (insertMismatchInOwningSequence(modToApply, isoform, movedStart)) {
            return;
        }

        if (checkForSameModificationInCanocicalSequence(modToApply, isoform, movedStart)) {
            return;
        }
        /// search overlapping gaps in canonical sequence

        if (checkForSameModificationInThisSequence(modToApply, isoform, movedStart)) {
//            return;
        }
         checkForEndOverlapsInThisSequence(modToApply, isoform, movedStart);
//        /// overlap in modified sequences

        //System.out.println("inserting gap in " + modToApply);
        int sub = modToApply.getSubstitution() == null ? 0 : modToApply.getSubstitution().length();
        if (sub > modToApply.getEnd() - modToApply.getBegin()) {

            SequenceFeature gap = new SequenceFeature();
            gap.setStart(modToApply.getBegin() + movedStart);
            int modToApplyEnd = modToApply.getEnd() + movedStart + sub;
//            int ownEnd = parentIsoform.
            gap.setEnd(modToApplyEnd);
            gap.setType("gapI");
//            addFeature(gap);
//            System.out.println(getId() + " :: " + (gap.getStart() - 1 - movedStart) + " to " + sequence.length());

            sequence = sequence.substring(0, (modToApply.getBegin() - 1))
                    + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart() - 1, "-"))
                    + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());

        }
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
    private boolean insertMismatchInOwningSequence(Isoforms.Modification modToApply, Isoform isoform, int movedStart) {
        if (getId().equals(isoform.getId())) {
            if (modToApply.getSubstitution() != null
                    && modToApply.getSubstitution().length() <= (modToApply.getEnd() - modToApply.getBegin())) {

                SequenceFeature mismatch = new SequenceFeature();
                mismatch.setStart(modToApply.getBegin() + movedStart);
                mismatch.setEnd(modToApply.getBegin() + modToApply.getSubstitution().length() + movedStart);
                mismatch.setType("mismatch");
                addFeature(mismatch);
            }
            return true;
        }
        return false;
    }

    private boolean checkForSameModificationInCanocicalSequence(Isoforms.Modification modToApply, Isoform isoform, int movedStart) {
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
    private boolean checkForSameModificationInThisSequence(Isoforms.Modification modToApply, Isoform isoform, int movedStart) {
        String subToApply = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
        if (parentIsoform.getModifications() == null) {
            return false;
        }
        for (Isoforms.Modification ownModification : parentIsoform.getModifications()) {
            if (modToApply.getBegin() == ownModification.getBegin()
                    && modToApply.getEnd() == ownModification.getEnd()
                    && subToApply.equals(ownModification.getSubstitution())) {
                logger.trace("{} has the same modification {} as {}", getId(), modToApply, isoform.getId());
                return true;
            }
        }

        return false;
    }

    private boolean checkForEndOverlapsInThisSequence(Isoforms.Modification modToApply, Isoform isoform, int movedStart) {
        String subToApply = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
        if (parentIsoform.getModifications() == null) {
            return false;
        }
        for (Isoforms.Modification ownModification : parentIsoform.getModifications()){
           
            String modSub = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
            int modRealEnd = modToApply.getBegin() + modSub.length() > modToApply.getEnd() ? modToApply.getBegin() + modSub.length() : modToApply.getEnd();
            String ownSub = ownModification.getSubstitution() != null? ownModification.getSubstitution() : "";
            int ownRealEnd = ownModification.getBegin() + ownSub.length() > ownModification.getEnd()?ownModification.getBegin() + ownSub.length() :ownModification.getEnd();
            
//             System.out.println(isoform.getId()+ "  " + modRealEnd + " -<- " + ownRealEnd + " " + this.getId());
            if (modToApply.getBegin() <= ownModification.getEnd()
                    && modRealEnd >= ownRealEnd ){ 
               
                SequenceFeature gap = new SequenceFeature();
                gap.setStart(modToApply.getBegin() + ownSub.length());
                gap.setEnd(modRealEnd);
                gap.setType("gap");
                addFeature(gap);
//                 System.out.println("found overlap " + gap + " for " + getId()) ;
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

    public void addFeature(SequenceFeature feature) {
        if (features == null) {
            features = new ArrayList<>();
        }
        features.add(feature);
    }

}
