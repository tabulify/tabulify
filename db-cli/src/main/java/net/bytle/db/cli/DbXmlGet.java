package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.xml.Xmls;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


public class DbXmlGet {


    public static final String FILE_XML = "file.xml";
    public static final String XPATH = "xpath";

    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.optionOf(XPATH)
                .setDescription("xpath");

        cliCommand.argOf(FILE_XML)
                .setDescription("Xml file");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String inputFilePath = cliParser.getString(FILE_XML);
        String xpath = cliParser.getString(XPATH);

        try {

            InputStream inputStream = Files.newInputStream(Paths.get(inputFilePath));
            System.out.println(Xmls.get(inputStream,xpath));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }



    private static void usage() {
        System.err.println("Usage: xmlGet [-options] <file.xml>");
        System.err.println("       -xpath = Xpath");
        System.err.println("       -usage or -help = this message");
        System.exit(1);
    }

}
