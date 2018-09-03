package de.sybig.uniprotFetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
@javax.ws.rs.Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class Isoforms {

    private static final Logger logger = LoggerFactory.getLogger(Isoforms.class);
    private final UniProtConfiguration configuration;
    private static final int SVG_LINE_HEIGHT = 25;
//    private Document document;

    Isoforms(UniProtConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    @javax.ws.rs.Path("/isoforms/{uniprotID}")
    public List<Isoform> getIsoforms(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        Document doc = getDocument(uniprotID);
        List<Isoform> isoforms = new LinkedList<>();
        isoforms.add(getCanonicalSequence(doc));
        isoforms.addAll(getModifiedSequences(doc));
        logger.trace("Got {} isoforms for {}", isoforms.size(), uniprotID);
        return isoforms;
    }

    @GET
    @javax.ws.rs.Path("/isoforms/alignmentPos/{uniprotID}")
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
                    as.applyModification(m, isoform, sequences);  // the current modification and the parent isoform
                }
//                break;
            }
        }
        return sequences;
    }

    @GET
    @javax.ws.rs.Path("/isoforms/svg/{uniprotID}/{sequence}")
    @Produces("image/svg+xml")
    public String getSVGWithSequence(
            @PathParam(value = "uniprotID") String uniprotID,
            @PathParam(value = "sequence") String sequence,
            @QueryParam("color") String color)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        int width = 1000;
        List<AlignedSequence> alignment = getAlignmentPos(uniprotID);

        StringBuilder svg = new StringBuilder();
        svg = addSVGStart(svg, width, alignment.size());
        addDBD(svg, sequence, alignment, width, validateColor("#" + color));
        svg = addAlignmentsToSVG(svg, alignment, width);
        svg = addSVGEnd(svg);
        return svg.toString();
    }

    private String validateColor(String color) {
        if (color == null) {
            color = "#AAAAAA";
        }
        Pattern pattern = Pattern.compile("^#[0-9A-Fa-f]{6}$");
        Matcher matcher = pattern.matcher(color);
        if (!matcher.find()) {
            color = "#AAAAAA";
        }
        return color;
    }

    @GET
    @javax.ws.rs.Path("/isoforms/svg/{uniprotID}")
    @Produces("image/svg+xml")
    public String getSVG(@PathParam(value = "uniprotID") String uniprotID) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        int width = 1000;
        List<AlignedSequence> alignment = getAlignmentPos(uniprotID);

//        System.out.println("aa size " + aaSize);
        StringBuilder svg = new StringBuilder();
        svg = addSVGStart(svg, width, alignment.size());
        svg = addAlignmentsToSVG(svg, alignment, width);
        svg = addSVGEnd(svg);
        return svg.toString();

    }

    @GET
    @javax.ws.rs.Path("/best/{uniprotIDs}")
    @Produces(MediaType.TEXT_PLAIN)
    public String selectBest(@PathParam(value = "uniprotIDs") String uniprotIDs) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {

        List<UniProtQuality> items = new LinkedList<>();
        XPathFactory xPathfactory = XPathFactory.newInstance();

        for (String id : uniprotIDs.split(",")) {
            UniProtQuality prot = new UniProtQuality();
            prot.setId(id);

            Document doc = getDocument(id);

            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/RDF/Description/reviewed");
            Node reviewedNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (reviewedNode == null || reviewedNode.getTextContent().equals("false")) {
                prot.setReviewed(false);
            } else {
                prot.setReviewed(true);
            }

            xpath = xPathfactory.newXPath();
            expr = xpath.compile("/RDF/Description/obsolete");
            Node obsoleteNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (obsoleteNode == null || reviewedNode.getTextContent().equals("false")) {
                prot.setObsolete(false);
            } else {
                prot.setObsolete(true);
            }

            xpath = xPathfactory.newXPath();
            expr = xpath.compile("/RDF/Description/existence");
            Node evidenceNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (evidenceNode != null) {
//                System.out.println("node " + evidenceNode);
//                System.out.println("atts " + evidenceNode.getAttributes());
//                System.out.println("res " + evidenceNode.getAttributes().getNamedItem("rdf:resource"));
                prot.setLevel(evidenceNode.getAttributes().getNamedItem("rdf:resource").getNodeValue());
            }
            items.add(prot);
        }
        items.sort(new UniprotQualityComparator());
        System.out.println("sorted list " + items);
        return items.get(0).getId();

    }

    private StringBuilder addSVGStart(StringBuilder svg, int width, int lines) {
        svg.append(String.format("<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" onload=\"init(evt)\">\n", width, (lines * SVG_LINE_HEIGHT +30) ));
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
                + "  tooltip.setAttributeNS(null,\"x\",11);\n"
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
                    + "  <rect x = \"5\" y = \"%d\" width = \"%d\" height = \"20\" stroke = \"none\" fill = \"#FFCC62\"/>\n",
                    ypos, (int) (aaSize * sequence.getSequence().length())
            ));
            
            for (SequenceFeature feature : sequence.getFeatures()) {
                String color = null;
                if ("gap".equals(feature.getType())) {
                    color = "EEEEEE";
                } else if ("mismatch".equals(feature.getType())) {
                    color = "FFAAAA";
                } else if ("gapD".equals(feature.getType())) {
                    color = "EEEEEE";
                } else if ("gapI".equals(feature.getType())) {
                    color = "EEEEEE";
                }

                if (color == null) {
                    continue;
                }
                String tooltip = String.format("%s: %d - %d (%d)", feature.getType(), feature.getStart()-feature.getMovedStart(), 
                        feature.getEnd() - feature.getMovedStart(), feature.getLength());
                svg.append(String.format("  <rect x = \"%d\" y = \"%d\" width = \"%d\" height = \"18\" "
                        + "stroke = \"none\" fill = \"#%s\" "
                        + "onmousemove=\"ShowTooltip(evt, '%s')\"\n"
                        + "    onmouseout=\"HideTooltip()\" /> \n",
                        (int) (feature.getStart() * aaSize + 5),
                        ypos + 1, (int) (aaSize * (feature.getLength())), color,
                        tooltip));
            }
            svg.append(String.format("  <text x=\"%d\" y=\"%d\" font-family=\"Verdana\" font-size=\"10\" fill=\"blue\">%s</text>\n</g>\n\n",
                    width - 60, ypos + 15, sequence.getId()));
            ypos += SVG_LINE_HEIGHT;
        }
        return svg;

    }

    private StringBuilder addDBD(StringBuilder svg, String sequence, List<AlignedSequence> alignment, int width, String color) {
        String canonicalSequence = alignment.get(0).getSequence();
        String origSequence = canonicalSequence.replace("-", "");
        double aaSize = ((double) width) / (getMaxSequenceLength(alignment));

        int start = origSequence.indexOf(sequence);
        if (start < 0) {
            logger.error("Could not find DBD for {}", alignment.get(0).getId());
            return svg;
        }
        for (int pos = 0; pos < start; pos++) {
            if ("-".equals(canonicalSequence.charAt(pos))) {
                start++;
            }
        }
        int end = start + sequence.length();
        //System.out.println("moved start " + start + " " + end);
        for (int pos = start; pos < end; pos++) {
            if ("-".equals(canonicalSequence.charAt(pos))) {
                end++;
            }
        }
        int height = 25 * alignment.size() + 20;
        svg.append(String.format("  <rect x = \"%d\" y = \"%d\" width = \"%d\" height = \"%d\" stroke = \"none\" fill = \"%s\"/>\n",
                (int) (aaSize * start), 0, (int) (aaSize * end - start), height, color));

        logger.debug("   found at " + start + " --- " + end);
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

    private Document getDocument(String uniprotID) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {

        Path rdfFile = getRDFfile(uniprotID);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(rdfFile.toFile());

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/RDF/Description/replacedBy");
        Node replaceNode = (Node) expr.evaluate(document, XPathConstants.NODE);
        if (replaceNode != null) {
            String newID = replaceNode.getAttributes().getNamedItem("rdf:resource").getNodeValue();
            newID = newID.substring(newID.lastIndexOf("/") + 1);
            System.out.println(uniprotID + " replaced by " + newID);
            return getDocument(newID);
        }

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
            } else if ("basedOn".equals(name)){
                isoform.setBasedOn(child.getAttributes().getNamedItem("rdf:resource").getNodeValue().replace("http://purl.uniprot.org/isoforms/", ""));
            } 
            else if ("name".equals(name)) {
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

    private Path getRDFfile(String id) throws MalformedURLException, IOException {

        Path localFile = getLocalFile(id);

        if (!Files.isReadable(localFile)) {
            logger.debug("getting from uniprot {}", id);
            FileUtils.copyURLToFile(new URL("http://www.uniprot.org/uniprot/" + id + ".rdf"),
                    getLocalFile(id).toFile(), 10 * 1000, 10 * 1000); // 10 seconds connectionTimeout and 10 seconds readTimeout
            return localFile;
        }

        FileTime validTime = FileTime.fromMillis(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * new Long(90)));

        if (Files.readAttributes(localFile, BasicFileAttributes.class).lastModifiedTime().compareTo(validTime) < 1) {
            logger.info("File {} is to old, will be refetched from uniprot.", localFile);
            try {
                FileUtils.copyURLToFile(new URL("https://www.uniprot.org/uniprot/" + id + ".rdf"),
                        getLocalFile(id).toFile(), 1500, 10 * 1000); // 1.5 seconds connectionTimeout and 10 seconds readTimeout
            } catch (java.net.SocketTimeoutException ex) {
                logger.warn("Could not re-fetch {} from Uniprot in max 1.5 secs, using old file.");
            }
            return localFile;
        }

        return localFile;
    }

    private Path getLocalFile(String id) {
        Path file = Paths.get(configuration.getDataDir(), id + ".rdf");
        return file;
    }



    class UniProtQuality {

        private String id;
        private boolean obsolete = false;
        private boolean reviewed = false;
        private int level = 0;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isObsolete() {
            return obsolete;
        }

        public void setObsolete(boolean obsolete) {
            this.obsolete = obsolete;
        }

        public boolean isReviewed() {
            return reviewed;
        }

        public void setReviewed(boolean reviewed) {
            this.reviewed = reviewed;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public void setLevel(String level) {
            if ("http://purl.uniprot.org/core/Evidence_at_Protein_Level_Existence".equals(level)) {
                this.level = 1;
            } else if ("http://purl.uniprot.org/core/Evidence_at_Transcript_Level_Existence".equals(level)) {
                this.level = 2;
            } else if ("http://purl.uniprot.org/core/Inferred_from_Homology_Existence".equals(level)) {
                this.level = 3;
            } else if ("http://purl.uniprot.org/core/Predicted_Existence".equals(level)) {
                this.level = 4;
            } else {
                System.out.println("level not found " + level);
            }
        }

        public String toString() {
            return String.format("%s {%b %d}", id, reviewed, level);
        }
        //1. Experimental evidence at protein level   <existence rdf:resource="http://purl.uniprot.org/core/Evidence_at_Protein_Level_Existence"/>
//2. Experimental evidence at transcript level    <existence rdf:resource="http://purl.uniprot.org/core/Evidence_at_Transcript_Level_Existence"/>
//3. Protein inferred from homology  http://purl.uniprot.org/core/Inferred_from_Homology_Existence
//4. Protein predicted http://purl.uniprot.org/core/Predicted_Existence
//5. Protein uncertain
    }

    class UniprotQualityComparator implements Comparator<UniProtQuality> {

        @Override
        public int compare(UniProtQuality o1, UniProtQuality o2) {
            if (o1.isObsolete() && !o2.isObsolete()) {
                return 1;
            }
            if (!o1.isObsolete() && o2.isObsolete()) {
                return -1;
            }
            if (o1.isReviewed() && !o2.isReviewed()) {
                return -1;
            }
            if (!o1.isReviewed() && o2.isReviewed()) {
                return 1;
            }
            return (((Integer) o1.getLevel()).compareTo((Integer) o2.getLevel()));
        }

    }
}
