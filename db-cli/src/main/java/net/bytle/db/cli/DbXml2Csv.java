package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.xml.Xmls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Created by gerard on 31-05-2017.
 */
public class DbXml2Csv {

    public static final String XPATH = "xpath";
    public static final String XML_INPUT = "xml_input";
    private static Logger LOGGER;


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.optionOf(XPATH)
                .setDescription("defines the Xpath Expression.")
                .setMandatory(true);

        cliCommand.argOf(XML_INPUT)
                .setDescription("defines the Xml File Input")
                .setMandatory(true);


        cliCommand.addExample(CliUsage.getFullChainOfCommand(cliCommand) + " --xpath \"//Repository/DECLARE/Database[@name=\\\"databaseName\\\"]/\" -in inputFile.xml \n");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String inputFilePath = cliParser.getString(XML_INPUT);
        String xpath = cliParser.getString(XPATH);


        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(Paths.get(inputFilePath));

        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
        Xmls.xml2Csv(inputStream, xpath);

    }


}
