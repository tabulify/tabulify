package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.type.Strings;
import net.bytle.xml.Xmls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;


public class TabliXmlUpdate {


  private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
  public static final String XPATH = "xpath";
  public static final String SOURCE_XML_FILE = "in";
  public static final String TARGET_XML_FILE = "out";
  public static final String CSV_FILE = "csv";
  public static final String VALUE = "value";


  public static void run(CliCommand childCommand) {

    String example = "\nExample to change the value of the attribute \"dataSource\" to \"NewValue\" in the element \"ConnectionPool\" with the attribute \"name\" equal to \"myConnectionPool\" of the file \"inputFile\", you would call it as:\n" +
      CliUsage.getFullChainOfCommand(childCommand) + " -xp \"//Repository/DECLARE/ConnectionPool[@name=\\\"myConnectionPool\\\"]/@dataSource\" -val newDNS -in inputFile.xml \n";
    childCommand.addExample(example);


    childCommand.addProperty(XPATH)
      .setDescription("defines the Xpath Expression. It may be also given in the CSV file parameter.");

    childCommand.addProperty(VALUE)
      .setDescription("defines the new text value for the Xpath Expression. It may be also given in the CSV file parameter.");

    childCommand.addProperty(SOURCE_XML_FILE)
      .setDescription("defines the Xml File Input");

    childCommand.addProperty(TARGET_XML_FILE)
      .setDescription("defines the Xml File created (Default to a timestamped file)");

    childCommand.addProperty(CSV_FILE)
      .setDescription("defines the Csv File that contains a list of row (xpath, value) to check the XML file in batch");


    // Treatment of the args parameters
    CliParser cliParser = childCommand.parse();

    // Input File
    Path csvPath = cliParser.getPath(CSV_FILE);

    String xpath = cliParser.getString(XPATH);

    if (xpath == null && csvPath == null) {
      throw new IllegalArgumentException(
        Strings.createMultiLineFromStrings(
          "The xpath parameter (" + XPATH + ") or the CSV file parameter (" + CSV_FILE + ") is mandatory",
          "None of this parameters were found"
        ).toString());
    }

    Path inputFilePath = cliParser.getPath(SOURCE_XML_FILE);

    String value = cliParser.getString(VALUE);

    String outputFilePath = cliParser.getString(TARGET_XML_FILE);
    if (outputFilePath == null) {
      String timeStamp = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date());
      outputFilePath = inputFilePath.toString() + "_" + timeStamp + ".xml";
    }


    InputStream inputStream;
    OutputStream outputStream;
    try {
      inputStream = Files.newInputStream(inputFilePath);
      outputStream = Files.newOutputStream(Paths.get(outputFilePath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Xmls.update(inputStream, xpath, value, outputStream, csvPath);

    LOGGER.info("The xml input file was updated and written to " + outputFilePath);

  }


}
