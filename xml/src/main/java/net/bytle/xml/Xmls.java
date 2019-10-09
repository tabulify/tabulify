package net.bytle.xml;

import net.bytle.db.csv.CsvTable;
import net.bytle.db.csv.CsvSelectStream;
import net.bytle.db.stream.SelectStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static net.bytle.xml.Doms.LOGGER;

public class Xmls {

    /**
     * To be able to test
     *
     * @param inputStream
     * @param xpath
     * @param value
     * @param outputStream
     */
    public static void update(InputStream inputStream, String xpath, String value, OutputStream outputStream, Path csvPath) {

        Document doc = Doms.getDom(inputStream);

        // Batch check or single check
        if (csvPath == null) {
            LOGGER.info("Single check mode");
            updateOne(doc, xpath, value);
        } else {
            // CSV
            LOGGER.info("Batch check mode");

            CsvTable csvTable = CsvTable.of(csvPath);
            SelectStream selectStream = CsvSelectStream.of(csvTable);
            while (selectStream.next()) {
                String csvXpath = selectStream.getString(0);
                String csvValue = selectStream.getString(1);
                updateOne(doc, csvXpath, csvValue);
            }

        }
        Doms.toStream(doc, outputStream);

    }

    private static void updateOne(Document doc, String csvXpath, String csvValue) {
        NodeList nodeList = Doms.getNodeList(doc, csvXpath);
        Doms.updateText(nodeList, csvValue);
        LOGGER.info("A single check operation was performed with the parameters (xpath=" + csvXpath + ", value=" + csvValue + ")");
    }

    /**
     * @param inputStream
     * @param xpath
     */
    public static String get(InputStream inputStream, String xpath) {

        Document doc = Doms.getDom(inputStream);
        NodeList nodeList = Doms.getNodeList(doc, xpath);
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < nodeList.getLength(); i++) {
            String text = Doms.getText(nodeList.item(i));
            stringBuilder.append(text);
        }

        return stringBuilder.toString();

    }

    /**
     * To be able to test
     *
     * @param inputStream
     * @param xpath
     * @param value
     * @param printWriter
     */
    public static int check(InputStream inputStream, String xpath, String value, PrintWriter printWriter, Path csvPath) {

        Document doc = Doms.getDom(inputStream);


        // A pointer to count the number of error
        int nbError = 0;

        // Batch check or single check
        if (csvPath == null) {
            printWriter.println("Single check mode");
            nbError = checkOne(doc, xpath, value, printWriter);
        } else {
            // CSV
            printWriter.println("Batch check mode");
            try {
                Reader in = new FileReader(csvPath.toString());
                Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
                for (CSVRecord record : records) {
                    String csvXpath = record.get(0);
                    String csvValue = record.get(1);
                    int localLoopError = checkOne(doc, csvXpath, csvValue, printWriter);
                    if (localLoopError != 0) {
                        nbError += localLoopError;
                    }
                }
            } catch (FileNotFoundException e) {
                // reader function
                throw new RuntimeException(e);
            } catch (IOException e) {
                // parse function
                throw new RuntimeException(e);
            }

        }

        return nbError;

    }

    private static int checkOne(Document doc, String csvXpath, String csvValue, PrintWriter printWriter) {
        NodeList nodeList = Doms.getNodeList(doc, csvXpath);
        if (nodeList.getLength() == 0) {
            printWriter.println("Warning: The node defined by the xpath (" + csvXpath + ") was not found.");
            return 1;
        } else {
            String text = Doms.getText(nodeList.item(0));
            if (!csvValue.equals(text)) {
                printWriter.println("Error: The node defined by the xpath (" + csvXpath + ") has not the value (" + csvValue + ") but the value (" + text + ")");
                return 1;
            } else {
                printWriter.println("Ok: The node defined by the xpath (" + csvXpath + ") has the value (" + csvValue + ")");
                return 0;
            }
        }
    }

    // To store  the data
    public static List<Map<String, String>> records = new ArrayList<>();
    // To store the columns header
    public static Set<String> headers = new HashSet<>();

    public static void echo(Node n, Integer level, Map<String, String> record) {

        // Log
        LOGGER.fine(level + " " + Doms.getNodeInfo(n));

        int type = n.getNodeType();
        switch (type) {
            case Node.ATTRIBUTE_NODE:
                break;
            case Node.CDATA_SECTION_NODE:
                break;
            case Node.COMMENT_NODE:
                break;
            case Node.DOCUMENT_FRAGMENT_NODE:
                break;
            case Node.DOCUMENT_NODE:
                break;
            case Node.DOCUMENT_TYPE_NODE:

                // Print entities if any
                NamedNodeMap nodeMap = ((DocumentType) n).getEntities();

                for (int j = 0; j < nodeMap.getLength(); j++) {
                    Entity entity = (Entity) nodeMap.item(j);
                    echo(entity, level, record);
                }

                break;
            case Node.ELEMENT_NODE:


                // Print attributes if any.  Note: element attributes are not
                // children of ELEMENT_NODEs but are properties of their
                // associated ELEMENT_NODE.  For this reason, they are printed
                // with 2x the indent level to indicate this.
                NamedNodeMap atts = n.getAttributes();

                for (int j = 0; j < atts.getLength(); j++) {
                    Node att = atts.item(j);
                    // An attribute has its text in a child text node
                    headers.add(att.getNodeName());
                    record.put(att.getNodeName(), Doms.getText(att));
                }
                break;
            case Node.ENTITY_NODE:

                break;
            case Node.ENTITY_REFERENCE_NODE:

                break;
            case Node.NOTATION_NODE:

                break;
            case Node.PROCESSING_INSTRUCTION_NODE:

                break;
            case Node.TEXT_NODE:


                break;
            default:


                break;
        }


        // Print children if any
        for (Node child = n.getFirstChild(); child != null;
             child = child.getNextSibling()) {
            level++;
            echo(child, level, record);
        }

    }

    /**
     * From an Xpath location, this function will extract the content.
     * IF the node is unique, it will be used as root, otherwise a root node will be added
     * Actually, the content is printed to the console.out
     *
     * @param xpath
     * @param inputStream
     */
    public static void xml2Csv(InputStream inputStream, String xpath) {

        Document doc = Doms.getDom(inputStream);
        NodeList nodeList = Doms.getNodeList(doc, xpath);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);
            HashMap<String, String> record = new HashMap<>();
            records.add(record);
            echo(n, 0, record);
        }

        // CSV
        CSVPrinter csvFilePrinter;
        String lastXPathPart = xpath.substring(xpath.lastIndexOf("/") + 1, xpath.length());
        Path csvFileName = Paths.get(lastXPathPart + ".csv");
        try {
            //initialize FileWriter object
            FileWriter fileWriter = new FileWriter(csvFileName.toString());
            //Create the CSVFormat object with "\n" as a record delimiter
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator());
            //initialize CSVPrinter object
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {

            // Create CSV file header
            List csvRow = new ArrayList();
            for (String header : headers) {
                csvRow.add(header);
            }
            csvFilePrinter.printRecord(csvRow);

            for (Map<String, String> record : records) {
                csvRow = new ArrayList();
                for (String header : headers) {
                    csvRow.add(record.get(header));
                }
                csvFilePrinter.printRecord(csvRow);
            }

            // Close the CSV file
            csvFilePrinter.flush();
            csvFilePrinter.close();

            // Total Rows downloaded
            System.out.println(records.size() + " records where added to the file " + csvFileName.toAbsolutePath());

        } catch (IOException e) { // For all csv.printRecordMethod
            throw new RuntimeException(e);
        }


    }

    /**
     * From an Xpath location, this function will extract the Xml content
     * and print it to stdout
     *
     * IF the node is unique, it will be used as root, otherwise a root node will be added
     * Actually, the content is printed to the console.out
     *
     * @param xpath
     * @param inputFilePath
     *
     */
    public static void xmlExtract(Path inputFilePath, String xpath) {
        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(inputFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Document doc = Doms.getDom(inputStream);
        NodeList nodeList = Doms.getNodeList(doc, xpath);

        Document newXmlDocument;
        try {
            newXmlDocument = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        if (nodeList.getLength() == 1) {
            newXmlDocument.appendChild(newXmlDocument.importNode(nodeList.item(0), true));
        } else {
            Element root = newXmlDocument.createElement("root");
            newXmlDocument.appendChild(root);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Node copyNode = newXmlDocument.importNode(node, true);
                root.appendChild(copyNode);
            }
        }
        Doms.toStream(newXmlDocument, System.out);
    }
}

