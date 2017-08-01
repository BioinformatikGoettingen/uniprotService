package de.sybig.uniprotFetcher;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class SequenceFeature {
    
    int start;
    int end;
    String type;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
}
