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
//    private Document document;

    Isoforms(UniProtConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Path("/isoforms/{uniprotID}")
    public List<Isoform> getIsoforms(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        Document doc = getDocument(uniprotID);
        List<Isoform> isoforms = new LinkedList<Isoform>();
        isoforms.add(getCanonicalSequence(doc));
        isoforms.addAll(getModifiedSequences(doc));
        return isoforms;
    }

    @GET
    @Path("/isoforms/alignmentPos/{uniprotID}")
    public Object getAlignmentPos(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        List<Isoform> isoforms = getIsoforms(uniprotID);
        ArrayList<AlignedSequence> sequences = new ArrayList<>();
        for (Isoform isoform : isoforms) {
            sequences.add(new AlignedSequence(isoform.getSequence(), isoform.getId()));
        }
        for (Isoform isoform : isoforms){
            if (isoform.getModifications() == null){
                continue;
            }
            for (Modification m : isoform.getModifications()){
                for (AlignedSequence as : sequences){
                    as.apply(m, isoform.getId());
                }
//                break;
            }
        }
        return sequences;
    }

    private Document getDocument(String uniprotID) throws IOException, SAXException, ParserConfigurationException {

        File rdfFile = getRDFfile(uniprotID);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(rdfFile);

        return document;
    }

    private Isoform getCanonicalSequence(Document doc) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description/type[@resource='http://purl.uniprot.org/core/Simple_Sequence']/parent::Description");
        NodeList result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        Isoform isoform = new Isoform();

        if (result.getLength() > 0) {
            Node descpriptionNode = result.item(0);

            isoform = processSequenceNode(descpriptionNode, doc);
        }
        return isoform;
    }

    private List<Isoform> getModifiedSequences(Document doc) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description/type[@resource='http://purl.uniprot.org/core/Modified_Sequence']/parent::Description");
        NodeList modifiedSequences = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        List<Isoform> isoforms = new LinkedList<>();
        for (int i = 0; i < modifiedSequences.getLength(); i++) {
            Node isoformNode = modifiedSequences.item(i);
            isoforms.add(processSequenceNode(isoformNode, doc));
        }
        return isoforms;
    }

    private Isoform processSequenceNode(Node descpriptionNode, Document doc) throws XPathExpressionException {
        Isoform isoform = new Isoform();
        NodeList children = descpriptionNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if ("rdf:value".equals(name)) {
                isoform.setSequence(child.getTextContent());
            } else if ("name".equals(name)) {
                isoform.addName(child.getTextContent());
            } else if ("modification".equals(name)) {
                isoform.addModification(getModificationNode(child.getAttributes().getNamedItem("rdf:resource").getNodeValue(), doc));
            }
            String url = descpriptionNode.getAttributes().getNamedItem("rdf:about").getTextContent();
            isoform.setUrl(url);
            isoform.setId(url.substring(url.lastIndexOf("/") + 1));
        }
        return isoform;
    }

    private Modification getModificationNode(String uri, Document doc) throws XPathExpressionException {
        Modification modification = new Modification();
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description[@about='" + uri + "']");
        Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("substitution".equals(child.getNodeName())) {
                modification.setSubstitution(child.getTextContent());
            } else if ("range".equals(child.getNodeName())) {
                modification = getRange(modification, child.getAttributes().getNamedItem("rdf:resource").getNodeValue(), doc);
            }
        }
        return modification;
    }

    private Modification getRange(Modification modification, String uri, Document doc) throws XPathExpressionException {

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description[@about='" + uri + "']");
        Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("faldo:begin".equals(child.getNodeName())) {
                modification.setBegin(getPos(child.getAttributes().getNamedItem("rdf:resource").getNodeValue(), doc));
            }
            if ("faldo:end".equals(child.getNodeName())) {
                modification.setEnd(getPos(child.getAttributes().getNamedItem("rdf:resource").getNodeValue(), doc));
            }
        }

        return modification;
    }

    private int getPos(String uri, Document doc) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description[@about='" + uri + "']/position");
        Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        if (node == null) {
            return 0;
        }
        return Integer.parseInt(node.getTextContent());
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
        List<Modification> modifications;

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
    }

    public class Modification {

        String substitution;
        int begin;
        int end;

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
            return end +1;
        }

        public void setEnd(int end) {
            this.end = end;
        }
    }
}
