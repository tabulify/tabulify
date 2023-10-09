package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.xml.Xmls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TabliXmlGet {


    public static final String FILE_XML = "file.xml";
    public static final String XPATH = "xpath";

    public static void run(CliCommand cliCommand) {

        cliCommand.addProperty(XPATH)
                .setDescription("xpath");

        cliCommand.addArg(FILE_XML)
                .setDescription("Xml file");

        CliParser cliParser = cliCommand.parse();

        String inputFilePath = cliParser.getString(FILE_XML);
        String xpath = cliParser.getString(XPATH);

        try {

            InputStream inputStream = Files.newInputStream(Paths.get(inputFilePath));
            System.out.println(Xmls.get(inputStream,xpath));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }


}
