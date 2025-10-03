package com.tabulify.tabul;

import com.tabulify.Tabular;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;

import java.util.List;

import static com.tabulify.tabul.TabulWords.*;

/**
 * Created by gerard on 31-05-2017.
 * Should be moved to the {@link com.tabulify.xml.XmlDataPath}  module
 */
public class TabulXml {

  public static final String CSV = "csv";


  public static void run(Tabular tabular, CliCommand cliCommand, String[] args) {


    // A client command example
    String example = "To of the text value of the node defined by an Xpath expression, you would type:\n" +
      CliUsage.getFullChainOfCommand(cliCommand) + " of --xpath \"//Repository/DECLARE/Database[@name=\\\"databaseName\\\"]/\" -in inputFile.xml \n";


    cliCommand.addChildCommand(TabulWords.EXTRACT)
      .setDescription("extract one or multiple nodes and create another xml");
    cliCommand.addChildCommand(TabulWords.PRINT_COMMAND)
      .setDescription("print the XML tree as seen by the DOM");
    cliCommand.addChildCommand(TabulWords.CHECK)
      .setDescription("verify the value of a node or from an attribute");
    cliCommand.addChildCommand(TabulWords.UPDATE)
      .setDescription("update the value of a node or from an attribute");
    cliCommand.addChildCommand(TabulWords.GET)
      .setDescription("return the value of a node or from an attribute");
    cliCommand.addChildCommand(DESCRIBE_COMMAND)
      .setDescription("print a tree summary structure of an xml");
    cliCommand.addChildCommand(CSV)
      .setDescription("create from an XML file a CSV file");

    // Initiate the client helper
    cliCommand.setDescription("Xml utility")
      .addExample(example);

    CliParser cliParser = cliCommand.parse();

    List<CliCommand> commands = cliParser.getFoundedChildCommands();
    if (!commands.isEmpty()) {
      for (CliCommand command : commands) {
        switch (command.getName()) {
          case EXTRACT:
            TabulXmlExtract.run(command, args);
            break;
          case TabulWords.PRINT_COMMAND:
            TabulXmlPrint.run(command, args);
            break;
          case CHECK:
            TabulXmlCheck.run(tabular, command, args);
            break;
          case UPDATE:
            TabulXmlUpdate.run(command);
            break;
          case GET:
            TabulXmlGet.run(command);
            break;
          case DESCRIBE_COMMAND:
            TabulXmlStructure.run(command);
            break;
          case CSV:
            TabulXml2Csv.run(command);
            break;
          default:
            throw new IllegalArgumentException("The command (" + command + ") is unknown");
        }

      }
    } else {
      throw new IllegalArgumentException("A known command must be given");
    }

  }


}
