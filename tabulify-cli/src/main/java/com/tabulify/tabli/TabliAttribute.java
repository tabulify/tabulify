package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;

import java.nio.file.Path;
import java.util.List;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;
import static com.tabulify.tabli.TabliWords.*;


public class TabliAttribute {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.addChildCommand(SET_COMMAND)
      .setDescription("Set a attribute");
    childCommand.addChildCommand(LIST_COMMAND)
      .setDescription("List the attributes");
    childCommand.addChildCommand(DELETE_COMMAND)
      .setDescription("Delete a attribute");


    CliParser cliParser = childCommand.parse();
    List<DataPath> feedbackDataPaths = null;
    List<CliCommand> subChildCommands = cliParser.getFoundedChildCommands();
    if (subChildCommands.isEmpty()) {
      throw new IllegalArgumentException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
    } else {
      for (CliCommand subChildCommand : subChildCommands) {
        LOGGER_TABLI.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case LIST_COMMAND:
            feedbackDataPaths = TabliAttributeList.run(tabular, subChildCommand);
            break;
          case SET_COMMAND:
            feedbackDataPaths = TabliAttributeSet.run(tabular, subChildCommand);
            break;
          case DELETE_COMMAND:
            feedbackDataPaths = TabliAttributeDelete.run(tabular, subChildCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }

    return feedbackDataPaths;
  }


  /**
   * @param tabular   - the context
   * @param cliParser - the parser
   * @return the variable file path to modify
   */
  static Path getVariablesFilePathToModify(Tabular tabular, CliParser cliParser) {
    Path conf = cliParser.getPath(CONF_PATH_PROPERTY);
    if (conf != null) {
      return conf;
    }
    return tabular.getConfPath();
  }

}

