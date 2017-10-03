package de.sybig.uniprotFetcher;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class Isoform {

    private String id;
    private String url;
    private List<String> names;
    private String sequence;
    private List<Modification> modifications;
    private String basedOn;

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public void addName(String name) {
        if (this.names == null) {
            names = new ArrayList<>();
        }
        names.add(name);
    }

    public List<Modification> getModifications() {
        return modifications;
    }

    public void setModifications(List<Modification> modifications) {
        this.modifications = modifications;
    }

    public void addModification(Modification modificaton) {
        if (modifications == null) {
            modifications = new ArrayList<>();
        }
        modifications.add(modificaton);
    }

    public void setBasedOn(String basedOn) {
        System.out.println("based on " + basedOn);
        this.basedOn = basedOn;
    }

    public String getBasedOn() {
        return basedOn;
    }

}
