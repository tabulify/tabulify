package com.tabulify.tabul;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.xml.XmlStructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 * To print the structure of a XML based on its content
 */
public class TabulXmlStructure {

    public static final String XML_FILE = "XML_FILE";

    public static void run(CliCommand cliCommand) {

        cliCommand.addArg(XML_FILE)
                .setDescription("The Xml file")
                .setMandatory(true);

        CliParser cliParser = cliCommand.parse();

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
