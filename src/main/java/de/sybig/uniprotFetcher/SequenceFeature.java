package de.sybig.uniprotFetcher;

import java.util.Objects;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class SequenceFeature {

    int start;
    int end;
    String type;
    private int movedStart =0;

    public SequenceFeature() {
        super();
    }

    public SequenceFeature(Modification modToApply, int movedStart) {
        super();
        this.start = modToApply.getBegin() + movedStart;
        this.end = modToApply.getEnd() + movedStart;
    }

    public int getLength() {
        return end - start + 1;
    }

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

    public void setMovedStart(int movedStart) {
        this.movedStart = movedStart;
    }

    public int getMovedStart(){
        return movedStart;
    }
    
    @Override
    public String toString() {
        return String.format("%s from %d to %d", type, start, end);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.start;
        hash = 29 * hash + this.end;
        hash = 29 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SequenceFeature other = (SequenceFeature) obj;
        if (this.start != other.start) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return true;
    }

}
