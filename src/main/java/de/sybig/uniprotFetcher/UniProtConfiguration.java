package de.sybig.uniprotFetcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class UniProtConfiguration extends Configuration {

    private String dataDir;

    @JsonProperty
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    @JsonProperty
    public String getDataDir() {
        return dataDir;
    }
}
