package net.bytle.xml;

import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.*;

import static net.bytle.xml.Doms.outputEncoding;


/**
 * Inspired by the DOM echo example program of Edwin Goei
 *
 */
public class XmlDomTree {


    /**
     * Output goes here
     */
    private PrintWriter out;

    /**
     * Indent level
     */
    private int indent = 0;

    /**
     * Indentation will be in multiples of basicIndent
     */
    private final String basicIndent = "  ";



    public XmlDomTree() {
        OutputStreamWriter outWriter = null;
        try {
            outWriter = new OutputStreamWriter(System.out, outputEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        this.out = new PrintWriter(outWriter, true);
    }

    /**
     * Echo common attributes of a DOM2 Node and terminate output with an
     * EOL character.
     */
    public static String printlnCommon(Node n) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" nodeName=\"" + n.getNodeName() + "\"");

        String val = n.getNamespaceURI();
        if (val != null) {
            stringBuilder.append(" uri=\"" + val + "\"");
        }

        val = n.getPrefix();
        if (val != null) {
            stringBuilder.append(" pre=\"" + val + "\"");
        }

        val = n.getLocalName();
        if (val != null) {
            stringBuilder.append(" local=\"" + val + "\"");
        }

        val = n.getNodeValue();
        if (val != null) {
            stringBuilder.append(" nodeValue=");
            if (val.trim().equals("")) {
                // Whitespace
                stringBuilder.append("[WS]");
            } else {
                stringBuilder.append("\"" + n.getNodeValue() + "\"");
            }
        }
        return stringBuilder.toString();
    }

    public static XmlDomTree of() {
        return new XmlDomTree();
    }

    /**
     * Indent to the current level in multiples of basicIndent
     */
    private void outputIndentation() {
        for (int i = 0; i < indent; i++) {
            out.print(basicIndent);
        }
    }

    /**
     * Recursive routine to print out DOM tree nodes
     */
    public void echo(Node n) {
        // Indent to the current level before printing anything
        outputIndentation();

        int type = n.getNodeType();
        switch (type) {
            case Node.ATTRIBUTE_NODE:
                out.print("ATTR:");
                out.print(printlnCommon(n));
                break;
            case Node.CDATA_SECTION_NODE:
                out.print("CDATA:");
                out.print(printlnCommon(n));
                break;
            case Node.COMMENT_NODE:
                out.print("COMM:");
                out.print(printlnCommon(n));
                break;
            case Node.DOCUMENT_FRAGMENT_NODE:
                out.print("DOC_FRAG:");
                out.print(printlnCommon(n));
                break;
            case Node.DOCUMENT_NODE:
                out.print("DOC:");
                out.print(printlnCommon(n));
                break;
            case Node.DOCUMENT_TYPE_NODE:
                out.print("DOC_TYPE:");
                out.print(printlnCommon(n));

                // Print entities if any
                NamedNodeMap nodeMap = ((DocumentType) n).getEntities();
                indent += 2;
                for (int i = 0; i < nodeMap.getLength(); i++) {
                    Entity entity = (Entity) nodeMap.item(i);
                    echo(entity);
                }
                indent -= 2;
                break;
            case Node.ELEMENT_NODE:
                out.print("ELEM:");
                out.print(printlnCommon(n));

                // Print attributes if any.  Note: element attributes are not
                // children of ELEMENT_NODEs but are properties of their
                // associated ELEMENT_NODE.  For this reason, they are printed
                // with 2x the indent level to indicate this.
                NamedNodeMap atts = n.getAttributes();
                indent += 2;
                for (int i = 0; i < atts.getLength(); i++) {
                    Node att = atts.item(i);
                    echo(att);
                }
                indent -= 2;
                break;
            case Node.ENTITY_NODE:
                out.print("ENT:");
                out.print(printlnCommon(n));
                break;
            case Node.ENTITY_REFERENCE_NODE:
                out.print("ENT_REF:");
                out.print(printlnCommon(n));
                break;
            case Node.NOTATION_NODE:
                out.print("NOTATION:");
                out.print(printlnCommon(n));
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                out.print("PROC_INST:");
                out.print(printlnCommon(n));
                break;
            case Node.TEXT_NODE:
                out.print("TEXT:");
                out.print(printlnCommon(n));
                break;
            default:
                out.print("UNSUPPORTED NODE: " + type);
                out.print(printlnCommon(n));
                break;
        }

        // Print children if any
        indent++;
        for (Node child = n.getFirstChild(); child != null;
             child = child.getNextSibling()) {
            echo(child);
        }
        indent--;
    }



    // Error handler to report errors and warnings
    private static class MyErrorHandler implements ErrorHandler {
        /**
         * Error handler output goes here
         */
        private PrintWriter out;

        MyErrorHandler(PrintWriter out) {
            this.out = out;
        }

        /**
         * Returns a string describing parse exception details
         */
        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            String info = "URI=" + systemId +
                    " Line=" + spe.getLineNumber() +
                    ": " + spe.getMessage();
            return info;
        }

        // The following methods are standard SAX LocalErrorHandler methods.
        // See SAX documentation for more info.

        public void warning(SAXParseException spe) throws SAXException {
            out.println("Warning: " + getParseExceptionInfo(spe));
        }

        public void error(SAXParseException spe) throws SAXException {
            String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        public void fatalError(SAXParseException spe) throws SAXException {
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    }
}
