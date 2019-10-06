package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.xml.XmlStructure;

import java.io.*;


/**
 * To print the structure of a XML based on its content
 */
public class DbXmlStructure {

    public static final String XML_FILE = "XML_FILE";

    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(XML_FILE)
                .setDescription("The Xml file")
                .setMandatory(true);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        try {

            String filePath = cliParser.getString(XML_FILE);
            Reader reader = new InputStreamReader(new FileInputStream(filePath));
            XmlStructure.of(reader).printNodeNames();
            reader.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
