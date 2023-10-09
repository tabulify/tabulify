package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;

import java.util.List;

import static net.bytle.db.tabli.TabliWords.*;

/**
 * Created by gerard on 31-05-2017.
 */
public class TabliXml {

  public static final String CSV = "csv";


  public static void run(Tabular tabular, CliCommand cliCommand, String[] args) {


    // A client command example
    String example = "To of the text value of the node defined by an Xpath expression, you would type:\n" +
      CliUsage.getFullChainOfCommand(cliCommand) + " of --xpath \"//Repository/DECLARE/Database[@name=\\\"databaseName\\\"]/\" -in inputFile.xml \n";


    cliCommand.addChildCommand(TabliWords.EXTRACT)
      .setDescription("extract one or multiple nodes and create another xml");
    cliCommand.addChildCommand(TabliWords.PRINT_COMMAND)
      .setDescription("print the XML tree as seen by the DOM");
    cliCommand.addChildCommand(TabliWords.CHECK)
      .setDescription("verify the value of a node or from an attribute");
    cliCommand.addChildCommand(TabliWords.UPDATE)
      .setDescription("update the value of a node or from an attribute");
    cliCommand.addChildCommand(TabliWords.GET)
      .setDescription("return the value of a node or from an attribute");
    cliCommand.addChildCommand(STRUCTURE_COMMAND)
      .setDescription("print a tree summary structure of an xml");
    cliCommand.addChildCommand(CSV)
      .setDescription("create form an XML file a CSV file");

    // Initiate the client helper
    cliCommand.setDescription("Xml utility")
      .addExample(example);

    CliParser cliParser = cliCommand.parse();

    List<CliCommand> commands = cliParser.getFoundedChildCommands();
    if (commands.size() > 0) {
      for (CliCommand command : commands) {
        switch (command.getName()) {
          case EXTRACT:
            TabliXmlExtract.run(command, args);
            break;
          case TabliWords.PRINT_COMMAND:
            TabliXmlPrint.run(command, args);
            break;
          case CHECK:
            TabliXmlCheck.run(tabular, command, args);
            break;
          case UPDATE:
            TabliXmlUpdate.run(command);
            break;
          case GET:
            TabliXmlGet.run(command);
            break;
          case STRUCTURE_COMMAND:
            TabliXmlStructure.run(command);
            break;
          case CSV:
            TabliXml2Csv.run(command);
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
