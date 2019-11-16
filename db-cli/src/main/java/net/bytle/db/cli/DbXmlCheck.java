package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.xml.Doms;
import net.bytle.xml.Xmls;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DbXmlCheck {


    public static final String XPATH = "xpath";
    public static final String XML_FILE = "xml_file";
    public static final String CSV_FILE = "csv_file";
    public static final String VALUE = "value";


    public static void run(CliCommand cliCommand, String[] args) {

        String example = "\nExample to check that the value of the attribute \"dataSource\" is \"NewValue\" in the element \"ConnectionPool\" with the attribute \"name\" equal to \"myConnectionPool\" of the file \"inputFile\", you would call it as:\n" +
                CliUsage.getFullChainOfCommand(cliCommand) + " -xp \"//Repository/DECLARE/ConnectionPool[@name=\\\"myConnectionPool\\\"]/@dataSource\" -val newDNS -in inputFile.xml \n";
        String footer = "This application will check if some node (element or attribute) have the correct value\n";

        cliCommand.setFooter(footer)
                .addExample(example);

        cliCommand.optionOf(XPATH)
                .setDescription("defines the Xpath Expression. It may be also given in the CSV file parameter.");

        cliCommand.optionOf(VALUE)
                .setDescription("defines the text value that must be found at the Xpath Expression. It may be also given in the CSV file parameter.");

        cliCommand.argOf(XML_FILE)
                .setDescription("defines the Xml File Input");

        cliCommand.argOf(CSV_FILE)
                .setDescription("defines the Csv File that contains a list of row (xpath, value) to check the XML file in batch");


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Input File
        String inputFilePath = cliParser.getString(XML_FILE);

        // Input Xpath and Value
        // Is it a batch check or a single check
        Path csvPath = cliParser.getPath(CSV_FILE);

        String xpath = cliParser.getString(XPATH);

        if (xpath == null && csvPath == null) {
            System.err.println("The xpath parameter or the CSV file parameter is mandatory");
            System.err.println("None of this parameters were found");
            CliUsage.print(cliCommand);
            System.exit(1);
        }

        String value = null;
        if (xpath != null) {
            value = cliParser.getString(VALUE);
        }

        // Output
        OutputStream outputStream = System.out;
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(outputStream, Doms.outputEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        PrintWriter printWriter = new PrintWriter(outputStreamWriter, true);

        InputStream inputStream;

        try {
            inputStream = Files.newInputStream(Paths.get(inputFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int nbError = Xmls.check(inputStream, xpath, value, printWriter, csvPath);
        if (nbError != 0) {
            System.out.println(nbError + " Error(s) were found");
        } else {
            System.out.println("No Errors were found");
        }
        printWriter.close();
        System.exit(nbError);

    }


}
