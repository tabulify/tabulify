package net.bytle.db.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.xml.Xmls;
public class DbXmlExtract {


    public static final String FILE_URI = "file.xml";

    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(Words.XPATH)
                .setDescription("The xpath")
                .setMandatory(true);
        cliCommand.argOf(FILE_URI)
                .setDescription("A Xml file Uri")
                .setMandatory(true);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Path inputFilePath = cliParser.getPath(FILE_URI);
        String xpath = cliParser.getString(Words.XPATH);

        Xmls.xmlExtract(inputFilePath,xpath);


    }






}
