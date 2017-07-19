package de.sybig.uniprotFetcher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class Isoforms {

    private final UniProtConfiguration configuration;

    Isoforms(UniProtConfiguration configuration) {
        this.configuration = configuration;
    }
    
    
    @GET
    @Path("/isoforms/{uniprotID}")
    public String getIsoforms(@PathParam(value = "uniprotID") String uniprotID) throws IOException{
      
        File rdfFile = getRDFfile(uniprotID);
        return Long.toString(rdfFile.lastModified());
    }
    
    
    private File getRDFfile(String id) throws MalformedURLException, IOException{
        
        File localFile = getLocalRDFfile(id);
        if (localFile == null){
            FileUtils.copyURLToFile(new URL("http://www.uniprot.org/uniprot/"+id+".rdf"), 
                        getLocalFile(id), 10 * 1000, 10 * 1000); // 10 seconds connectionTimeout and 10 seconds readTimeout
        }
        return getLocalRDFfile(id);
    }

    private File getLocalRDFfile(String id) {
        File file = getLocalFile(id);  
        if (file.canRead()){
            return file;
        }
        return null;
    }
    
    private File getLocalFile(String id){
        File dataDir = new File(configuration.getDataDir());
        File file = new File(dataDir, id+".rdf");
        return file;
    }
}
