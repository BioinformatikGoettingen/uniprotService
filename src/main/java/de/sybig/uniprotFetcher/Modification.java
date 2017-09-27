package de.sybig.uniprotFetcher;

public class Modification {

    private String substitution;
    private int begin;
    private int end;
    private transient String id;

    public int getLength() {
        return end - begin + 1;
    }

    public String getSubstitution() {
        return substitution;
    }

    public void setSubstitution(String substitution) {
        this.substitution = substitution;
    }

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("Modification %s from %d to %d substitution %s", id, begin, end, substitution);
    }
}
