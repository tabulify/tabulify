package net.bytle.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

import static net.bytle.xml.Doms.outputEncoding;

public class XmlDoc {
    public static Document of(DocumentBuilderFactory dbf, String filename) {

        try {
            // Step 2: create a DocumentBuilder that satisfies the constraints
            // specified by the DocumentBuilderFactory
            DocumentBuilder documentBuilder = null;

            documentBuilder = dbf.newDocumentBuilder();


            // Set an LocalErrorHandler before parsing
            OutputStreamWriter errorWriter =
                    new OutputStreamWriter(System.err, outputEncoding);
            documentBuilder.setErrorHandler(new LocalErrorHandler(new PrintWriter(errorWriter, true)));

            // Step 3: parse the input file
            return documentBuilder.parse(new File(filename));

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
