package de.sybig.uniprotFetcher;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class Isoforms {
    
    
    @GET
    @Path("/isoforms/{uniprotID}")
    public String getIsoforms(@PathParam(value = "uniprotID") String uniprotID){
        return uniprotID;
    }
}
