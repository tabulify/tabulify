package com.tabulify.tabul;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.xml.Xmls;

import java.nio.file.Path;

public class TabulXmlExtract {


    public static final String FILE_URI = "file.xml";

    public static void run(CliCommand cliCommand, String[] args) {

      cliCommand.addArg(TabulWords.XPATH)
                .setDescription("The xpath")
                .setMandatory(true);
        cliCommand.addArg(FILE_URI)
                .setDescription("A Xml file Uri")
                .setMandatory(true);

        CliParser cliParser = cliCommand.parse();

        Path inputFilePath = cliParser.getPath(FILE_URI);
      String xpath = cliParser.getString(TabulWords.XPATH);

        Xmls.xmlExtract(inputFilePath,xpath);


    }






}
