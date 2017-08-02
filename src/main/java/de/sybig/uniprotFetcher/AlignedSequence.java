package de.sybig.uniprotFetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class AlignedSequence {

    String type = "modified";
    List<SequenceFeature> features;
    String id;
    String sequence;

    public AlignedSequence(String sequence, String id) {
        SequenceFeature sf = new SequenceFeature();
        setSequence(sequence);
        setId(id);
        sf.setStart(1);
        sf.setEnd(sequence.length());
        addFeature(sf);
    }

    void apply(Isoforms.Modification m, String id) {
        if ((m.getEnd() - m.getBegin()) > m.getSubstitution().length()) {
            applyDeletion(m, id);
        } else {
            applyInsertion(m, id);
        }

    }

    private void applyDeletion(Isoforms.Modification m, String id) {
        if (this.id != id) {
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
        gap.setType("gap");

        sequence = sequence.substring(0, (m.getBegin() + sub - 1))
                + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart(), "-"))
                + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());

        addFeature(gap);
    }

    private void applyInsertion(Isoforms.Modification m, String id) {
        if (this.id == id) {
            return;
        }
        int movedStart = 0;
        for (SequenceFeature sf : features) {
            if ("gap".equals(sf.getType()) && sf.getStart() < m.getBegin()) {
                movedStart = movedStart + sf.getEnd() - sf.getStart();
            }
        }
        
        
        
        /// search overlapping gaps
        for (SequenceFeature sf : features){  //TODO considere moved start
            System.out.println("testing " +sf.getEnd() + "  -- "+ m.getBegin() + m.getSubstitution().length());
            if ("gap".equals(sf.getType()) && (m.getBegin() >= (sf.getStart() )) && ((m.getBegin() + m.getSubstitution().length()) <= (sf.getEnd() ))){
                System.out.println("no new feature");
                return;
            }
            
//            if (m.getBegin() < sf.getStart() && m.getEnd() > sf.getStart()){
//                SequenceFeature gap = new SequenceFeature();
//                gap.setStart(m.getBegin()+movedStart);
//                gap.setEnd(sf.getStart());
//                gap.setType("gap");
//                addFeature(gap);
//            }
                
        }
        ///
        

        int sub = m.getSubstitution() == null ? 0 : m.getSubstitution().length();
        SequenceFeature gap = new SequenceFeature();
        gap.setStart(m.getBegin() + movedStart);
        gap.setEnd(m.getBegin() + sub + movedStart);
        gap.setType("gap");
        addFeature(gap);

        sequence = sequence.substring(0, (m.getBegin() - 1))
                + String.join("", Collections.nCopies(gap.getEnd() - gap.getStart(), "-"))
                + sequence.substring(gap.getStart() - 1 - movedStart, sequence.length());

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
