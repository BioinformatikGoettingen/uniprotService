package de.sybig.uniprotFetcher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class Isoforms {

    private final UniProtConfiguration configuration;
    private Document document;

    Isoforms(UniProtConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Path("/isoforms/{uniprotID}")
    public List<Isoform> getIsoforms(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        List<Isoform> isoforms = new LinkedList<Isoform>();
        isoforms.add(getCanonicalSequence(uniprotID));
        isoforms.addAll(getModifiedSequences(uniprotID));
        return isoforms;
    }

    private Document getDocument(String uniprotID) throws IOException, SAXException, ParserConfigurationException {
        if (document == null) {
            File rdfFile = getRDFfile(uniprotID);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(rdfFile);
        }
        return document;
    }

    private Isoform getCanonicalSequence(String uniprotID) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        Document doc = getDocument(uniprotID);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description/type[@resource='http://purl.uniprot.org/core/Simple_Sequence']/parent::Description");
        NodeList result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        Isoform isoform = new Isoform();

        if (result.getLength() > 0) {
            Node descpriptionNode = result.item(0);

            isoform = processSequenceNode(descpriptionNode);
        }
        return isoform;
    }

    private List<Isoform> getModifiedSequences(String uniprotID) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        Document doc = getDocument(uniprotID);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description/type[@resource='http://purl.uniprot.org/core/Modified_Sequence']/parent::Description");
        NodeList modifiedSequences = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        List<Isoform> isoforms = new LinkedList<>();
        for (int i = 0; i < modifiedSequences.getLength(); i++) {
            Node isoformNode = modifiedSequences.item(i);
            isoforms.add(processSequenceNode(isoformNode));
        }
        return isoforms;
    }

    private Isoform processSequenceNode(Node descpriptionNode) {
        Isoform isoform = new Isoform();
        NodeList children = descpriptionNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if ("rdf:value".equals(name)) {
                isoform.setSequence(child.getTextContent());
            } else if ("name".equals(name)) {
                isoform.addName(child.getTextContent());
            }
            String url = descpriptionNode.getAttributes().getNamedItem("rdf:about").getTextContent();
            isoform.setUrl(url);
            isoform.setId(url.substring(url.lastIndexOf("/") + 1));
        }
        return isoform;
    }

    private File getRDFfile(String id) throws MalformedURLException, IOException {

        File localFile = getLocalRDFfile(id);
        if (localFile == null) {
            FileUtils.copyURLToFile(new URL("http://www.uniprot.org/uniprot/" + id + ".rdf"),
                    getLocalFile(id), 10 * 1000, 10 * 1000); // 10 seconds connectionTimeout and 10 seconds readTimeout
        }
        return getLocalRDFfile(id);
    }

    private File getLocalRDFfile(String id) {
        File file = getLocalFile(id);
        if (file.canRead()) {
            return file;
        }
        return null;
    }

    private File getLocalFile(String id) {
        File dataDir = new File(configuration.getDataDir());
        File file = new File(dataDir, id + ".rdf");
        return file;
    }

    class Isoform {

        String id;
        String url;
        List<String> names;
        String sequence;

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

    }
}
