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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(Isoforms.class);
    private final UniProtConfiguration configuration;
//    private Document document;

    Isoforms(UniProtConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Path("/isoforms/{uniprotID}")
    public List<Isoform> getIsoforms(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        Document doc = getDocument(uniprotID);
        List<Isoform> isoforms = new LinkedList<>();
        isoforms.add(getCanonicalSequence(doc));
        isoforms.addAll(getModifiedSequences(doc));
        logger.trace("Got {} isoforms for {}", isoforms.size(), uniprotID);
        return isoforms;
    }

    @GET
    @Path("/isoforms/alignmentPos/{uniprotID}")
    public List<AlignedSequence> getAlignmentPos(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        List<Isoform> isoforms = getIsoforms(uniprotID);
        ArrayList<AlignedSequence> sequences = new ArrayList<>();
        // Init all sequence objects with the sequence as single feature
        for (Isoform isoform : isoforms) {
            sequences.add(new AlignedSequence(isoform.getSequence(), isoform));
        }
        for (Isoform isoform : isoforms) {
            if (isoform.getModifications() == null) {
                continue;
            }
            for (Modification m : isoform.getModifications()) {
                for (AlignedSequence as : sequences) {
                    logger.trace("Applying modification '{}' to sequence {}", m, as.getId());
                    as.applyModification(m, isoform);  // the current modification and the parent isoform
                }
//                break;
            }
        }
        return sequences;
    }

    @GET
    @Path("/isoforms/svg/{uniprotID}/{sequence}")
    @Produces("image/svg+xml")
    public String getSVGWithSequence(@PathParam(value = "uniprotID") String uniprotID, @PathParam(value = "sequence") String sequence) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        int width = 1000;
        List<AlignedSequence> alignment = getAlignmentPos(uniprotID);

//        System.out.println("aa size " + aaSize);
        StringBuilder svg = new StringBuilder();
        svg = addSVGStart(svg, width);
        addDBD(svg, sequence, alignment, width);
        svg = addAlignmentsToSVG(svg, alignment, width);
        svg = addSVGEnd(svg);
        return svg.toString();
    }

    @GET
    @Path("/isoforms/svg/{uniprotID}")
    @Produces("image/svg+xml")
    public String getSVG(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        int width = 1000;
        List<AlignedSequence> alignment = getAlignmentPos(uniprotID);

//        System.out.println("aa size " + aaSize);
        StringBuilder svg = new StringBuilder();
        svg = addSVGStart(svg, width);
        svg = addAlignmentsToSVG(svg, alignment, width);
        svg = addSVGEnd(svg);
        return svg.toString();

    }

    private StringBuilder addSVGStart(StringBuilder svg, int width) {
        svg.append(String.format("<svg width=\"%d\" height=\"200\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" onload=\"init(evt)\">\n", width));
        svg.append("<script type=\"text/ecmascript\">\n"
                + "<![CDATA[\n"
                + "  function init(evt)\n"
                + "  {\n"
                + "    if ( window.svgDocument == null )\n"
                + "    {\n"
                + "      svgDocument = evt.target.ownerDocument;\n"
                + "    }\n"
                + "    tooltip = svgDocument.getElementById('tooltip');\n"
                + "  }\n"
                + "function ShowTooltip(evt, mouseovertext)\n"
                + "{\n"
                + "  tooltip.setAttributeNS(null,\"x\",evt.clientX+11);\n"
                + "  tooltip.setAttributeNS(null,\"y\",evt.clientY+27);\n"
                + "  tooltip.firstChild.data = mouseovertext;\n"
                + "  tooltip.setAttributeNS(null,\"visibility\",\"visible\");\n"
                + "}\n"
                + "\n"
                + "function HideTooltip()\n"
                + "{\n"
                + "  tooltip.setAttributeNS(null,\"visibility\",\"hidden\");\n"
                + "}"
                + "]]></script>");
        return svg;
    }

    private StringBuilder addAlignmentsToSVG(StringBuilder svg, List<AlignedSequence> alignment, int width) {
        int ypos = 10;
        double aaSize = ((double) width) / (getMaxSequenceLength(alignment));
        for (AlignedSequence sequence : alignment) {
            svg.append(String.format("<g>\n"
                    + "  <rect x = \"5\" y = \"%d\" width = \"%d\" height = \"20\" stroke = \"none\" fill = \"#FFBF00\"/>\n",
                    ypos, (int) (aaSize * sequence.getSequence().length())
            ));

            for (SequenceFeature feature : sequence.getFeatures()) {
                String color = null;
                if ("gap".equals(feature.getType())) {
                    color = "FFFFFF";
                } else if ("mismatch".equals(feature.getType())) {
                    color = "CCCCCC";
                } else if ("gapD".equals(feature.getType())) {
                    color = "FF0000";
                } else if ("gapI".equals(feature.getType())) {
                    color = "00FF00";
                }

                if (color == null) {
                    continue;
                }
                String tooltip = feature.getType() + ": " + feature.getStart() + " - " + feature.getEnd();
                svg.append(String.format("  <rect x = \"%d\" y = \"%d\" width = \"%d\" height = \"18\" "
                        + "stroke = \"none\" fill = \"#%s\" "
                        + "onmousemove=\"ShowTooltip(evt, '%s')\"\n"
                        + "    onmouseout=\"HideTooltip()\" /> \n",
                        (int) (feature.getStart() * aaSize + 5),
                        ypos + 1, (int) (aaSize * (feature.getEnd() - feature.getStart())), color,
                        tooltip));
            }
            svg.append(String.format("  <text x=\"%d\" y=\"%d\" font-family=\"Verdana\" font-size=\"10\" fill=\"blue\">%s</text>\n</g>\n\n",
                    width - 60, ypos + 15, sequence.getId()));
            ypos += 25;
        }
        return svg;

    }

    private StringBuilder addDBD(StringBuilder svg, String sequence, List<AlignedSequence> alignment, int width) {
        String canonicalSequence = alignment.get(0).getSequence();
        String origSequence = canonicalSequence.replace("-", "");
        double aaSize = ((double) width) / (getMaxSequenceLength(alignment));

        int start = origSequence.indexOf(sequence);
        if (start < 0){
            logger.error("Could not find DBD for {}", alignment.get(0).getId());
            return svg;
        }
        for (int pos = 0; pos < start; pos++) {
            if ("-".equals(canonicalSequence.charAt(pos))) {
                start++;
            }
        }
        int end = start + sequence.length();
        System.out.println("moved start " + start + " " + end);
        for (int pos = start; pos < end; pos++) {
            if ("-".equals(canonicalSequence.charAt(pos))) {
                end++;
            }
        }
        int height = 25 * alignment.size() + 20;
        svg.append(String.format("  <rect x = \"%d\" y = \"%d\" width = \"%d\" height = \"%d\" stroke = \"none\" fill = \"#AAAAAA\"/>\n",
                start, 0, (int) (aaSize * end - start), height
        ));

        logger.error("   found at " + start + " --- " + end);
        return svg;
    }

    private StringBuilder addSVGEnd(StringBuilder svg) {
        svg.append("<text class=\"tooltip\" id=\"tooltip\"\n"
                + "      x=\"0\" y=\"0\" visibility=\"hidden\">Tooltip</text>\n");
        svg.append("</svg>");
        return svg;
    }

    private int getMaxSequenceLength(List<AlignedSequence> alignment) {
        int maxLength = 0;
        for (AlignedSequence sequence : alignment) {
            maxLength = sequence.getSequence().length() > maxLength ? sequence.getSequence().length() : maxLength;
        }
        return maxLength;
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
        String url = descpriptionNode.getAttributes().getNamedItem("rdf:about").getTextContent();
        isoform.setUrl(url);
        isoform.setId(url.substring(url.lastIndexOf("/") + 1));
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
        modification.setId(uri.substring(uri.lastIndexOf("/") + 1));
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
        if (node == null) {
            System.out.println("no range found for " + uri);
        }
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

        private String id;
        private String url;
        private List<String> names;
        private String sequence;
        private List<Modification> modifications;

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

        private void setUniprotId(String substring) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public class Modification {

        private String substitution;
        private int begin;
        private int end;
        private transient String id;

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
            return end + 1;
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
}
