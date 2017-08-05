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
    private String id;
    private String sequence;
    
    private transient Isoform parentIsoform;
    
    public AlignedSequence(String sequence, String id) {
        SequenceFeature sf = new SequenceFeature();
        setSequence(sequence);
        setId(id);
        sf.setStart(1);
        sf.setEnd(sequence.length());
        addFeature(sf);
    }

    void applyModification(Isoforms.Modification m, Isoform isoform) {
        if ((m.getEnd() - m.getBegin()) > m.getSubstitution().length()) {
            applyDeletion(m, isoform);
        } else {
            applyInsertion(m, isoform);
        }

    }

    private void applyDeletion(Isoforms.Modification m, Isoform isoform) {
        if (this.id != isoform.getId()) {
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
        SequenceFeature gap = new SequenceFeature();
        gap.setStart(m.getBegin() + movedStart + sub);
        gap.setEnd(m.getEnd() + movedStart);
        gap.setType("gapD");
//        System.out.println(id + " changing sequnce from " + sequence.length());
        sequence = sequence.substring(0, (m.getBegin() + sub - 1))
                + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart(), "-"))
                + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());
//        System.out.println(" to " + sequence.length());
        addFeature(gap);
    }

    private void applyInsertion(Isoforms.Modification modToApply, Isoform isoform) {

        //  System.out.println("working on " + getId());
        int movedStart = 0;
        for (SequenceFeature sf : features) {
            if ("gap".equals(sf.getType()) && sf.getStart() < modToApply.getBegin()) {
                movedStart = movedStart + sf.getEnd() - sf.getStart();
            }
        }
        if (this.id == isoform.getId()) {
            if (modToApply.getSubstitution() != null
                    && modToApply.getSubstitution().length() <= (modToApply.getEnd() - modToApply.getBegin())) {
                SequenceFeature mismatch = new SequenceFeature();
                mismatch.setStart(modToApply.getBegin() + movedStart);
                mismatch.setEnd(modToApply.getBegin() + modToApply.getSubstitution().length() + movedStart);
                mismatch.setType("mismatch");
                addFeature(mismatch);
            }
            return;
        }

        /// search overlapping gaps in canonical sequence
        for (SequenceFeature sf : features) {  //TODO consider moved start
//            System.out.println("testing " +sf.getEnd() + "  -- "+ modToApply.getBegin() + modToApply.getSubstitution().length());
            if ("gap".equals(sf.getType()) && (modToApply.getBegin() >= (sf.getStart())) && ((modToApply.getBegin() + modToApply.getSubstitution().length()) <= (sf.getEnd()))) {
                System.out.println("no new feature " + isoform.getId());
                return;
            }
            // ToDo overalap at start
            //ToDo overlap at end

        }
//        /// overlap in modified sequences
        String subToApply = modToApply.getSubstitution() != null ? modToApply.getSubstitution() : "";
        for (Isoforms.Modification ownModification : isoform.getModifications()){
             if (modToApply.getBegin() == ownModification.getBegin()
                     && modToApply.getEnd() == ownModification.getEnd()
                     && subToApply.equals(ownModification.getSubstitution())){
                 logger.info(id + " have the same! " + isoform.getId());
                 return;
             }
        }
        //System.out.println("inserting gap in " + modToApply);

        int sub = modToApply.getSubstitution() == null ? 0 : modToApply.getSubstitution().length();
        if (sub > modToApply.getEnd() - modToApply.getBegin()) {

            SequenceFeature gap = new SequenceFeature();
            gap.setStart(modToApply.getBegin() + movedStart);
            gap.setEnd(modToApply.getEnd() + movedStart + sub);
            gap.setType("gapI");
            addFeature(gap);
//            System.out.println(id + " :: " + (gap.getStart() - 1 - movedStart) + " to " + sequence.length());
            
             sequence = sequence.substring(0, (modToApply.getBegin() - 1))
                + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart(), "-"))
                + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());

        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
