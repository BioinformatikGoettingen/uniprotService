package de.sybig.uniprotFetcher;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class UniProtApplication extends Application<UniProtConfiguration> {

    public static void main(String[] args) throws Exception {
        new UniProtApplication().run(args);
    }

    @Override
    public void run(UniProtConfiguration configuration, Environment environment) throws Exception {

        environment.jersey().register(new Isoforms(configuration));

    }

}
