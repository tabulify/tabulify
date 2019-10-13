package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.xml.LocalErrorHandler;
import net.bytle.xml.XmlDoc;
import net.bytle.xml.XmlDomTree;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static net.bytle.xml.Doms.outputEncoding;

/**
 * Created by gerard on 31-05-2017.
 * Inspired by the DOM echo example program of Edwin Goei
 */
public class DbXmlPrint {

    /**
     * Constants used for JAXP 1.2
     */
    static final String JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String W3C_XML_SCHEMA =
            "http://www.w3.org/2001/XMLSchema";
    static final String JAXP_SCHEMA_SOURCE =
            "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String FILE_XML = "file.xml";
    public static final String DTD_VALIDATION = "dtd";
    public static final String XSD_FILE = "xsd";
    public static final String IGNORE_WS = "ws";
    public static final String IGNORE_COMMENT = "comment";
    public static final String CDATA_INTO_TEXT = "cdata";
    public static final String CREATE_ENTITY_REF = "entity-ref";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(FILE_XML)
                .setDescription("A Xml file Uri");

        cliCommand.flagOf(DTD_VALIDATION)
                .setDescription("DTD validation")
                .setDefaultValue(false);

        cliCommand.optionOf(XSD_FILE)
                .setDescription("A xsd file Uri <file.xsd> = W3C XML Schema validation using xsi: hints in instance document or schema source <file.xsd>")
                .setShortName("xsdss");

        cliCommand.flagOf(IGNORE_WS)
                .setDescription("Create element content whitespace nodes")
                .setDefaultValue(false)
                ;
        cliCommand.flagOf(IGNORE_COMMENT)
                .setDescription("Create comment nodes")
                .setShortName("co")
                .setDefaultValue(false);

        cliCommand.flagOf(CDATA_INTO_TEXT)
                .setDescription("put CDATA into Text nodes")
                .setShortName("cd")
                .setDefaultValue(false);

        cliCommand.flagOf(CREATE_ENTITY_REF)
                .setDescription("create EntityReference nodes")
                .setShortName("e");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String filename = cliParser.getString(FILE_XML);
        boolean dtdValidate = cliParser.getBoolean(DTD_VALIDATION);
        String schemaSource = cliParser.getString(XSD_FILE);
        boolean xsdValidate = false;
        if (schemaSource!=null){
            xsdValidate=true;
        }

        boolean ignoreWhitespace = cliParser.getBoolean(IGNORE_WS);
        boolean ignoreComments = cliParser.getBoolean(IGNORE_COMMENT);
        boolean putCDATAIntoText = cliParser.getBoolean(CDATA_INTO_TEXT);
        boolean createEntityRefs = cliParser.getBoolean(CREATE_ENTITY_REF);

        // Step 1: create a DocumentBuilderFactory and configure it
        DocumentBuilderFactory documentBuilderFactory =
                DocumentBuilderFactory.newInstance();

        // Set namespaceAware to true to of a DOM Level 2 tree with nodes
        // containing namespace information.  This is necessary because the
        // default value from JAXP 1.0 was defined to be false.
        documentBuilderFactory.setNamespaceAware(true);

        // Set the validation mode to either: no validation, DTD
        // validation, or XSD validation
        documentBuilderFactory.setValidating(dtdValidate || xsdValidate);
        if (xsdValidate) {
            try {
                documentBuilderFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            } catch (IllegalArgumentException x) {
                // This can happen if the parser does not support JAXP 1.2
                System.err.println(
                        "Error: JAXP DocumentBuilderFactory attribute not recognized: "
                                + JAXP_SCHEMA_LANGUAGE);
                System.err.println(
                        "Check to see if parser conforms to JAXP 1.2 spec.");
                System.exit(1);
            }
        }

        // Set the schema source, if any.  See the JAXP 1.2 maintenance
        // updateText specification for more complex usages of this feature.
        if (schemaSource != null) {
            documentBuilderFactory.setAttribute(JAXP_SCHEMA_SOURCE, new File(schemaSource));
        }

        // Optional: set various configuration options
        documentBuilderFactory.setIgnoringComments(ignoreComments);
        documentBuilderFactory.setIgnoringElementContentWhitespace(ignoreWhitespace);
        documentBuilderFactory.setCoalescing(putCDATAIntoText);
        // The opposite of creating entity ref nodes is expanding them inline
        documentBuilderFactory.setExpandEntityReferences(!createEntityRefs);


        Document doc = XmlDoc.of(documentBuilderFactory, filename);

        // Print out the DOM tree
        XmlDomTree.of().echo(doc);

    }
}
