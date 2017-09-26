package de.sybig.uniprotFetcher;

/**
 *
 * @author juegen.doenitz@bioinf.med.uni-goettingen.de
 */
public class Range {

    int begin = -1;
    int end = -1;

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public boolean isValid() {
        if (begin > 0 && end > 0) {
            return true;
        }
        return false;
    }

    public int getLength() {
        return end - begin;
    }

    public String toString() {
        return String.format("Range %d to %d", begin, end);
    }
}
