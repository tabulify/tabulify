package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;

import java.nio.file.Path;
import java.util.List;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;
import static com.tabulify.tabli.TabliWords.*;


public class TabliVariable {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.addChildCommand(SET_COMMAND)
      .setDescription("Set a variable");
    childCommand.addChildCommand(LIST_COMMAND)
      .setDescription("List the variables");
    childCommand.addChildCommand(DELETE_COMMAND)
      .setDescription("Delete a variable");


    CliParser cliParser = childCommand.parse();
    List<DataPath> feedbackDataPaths = null;
    List<CliCommand> subChildCommands = cliParser.getFoundedChildCommands();
    if (subChildCommands.size() == 0) {
      throw new IllegalArgumentException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
    } else {
      for (CliCommand subChildCommand : subChildCommands) {
        LOGGER_TABLI.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case LIST_COMMAND:
            feedbackDataPaths = TabliVariableList.run(tabular, subChildCommand);
            break;
          case SET_COMMAND:
            feedbackDataPaths = TabliVariableSet.run(tabular, subChildCommand);
            break;
          case DELETE_COMMAND:
            feedbackDataPaths = TabliVariableDelete.run(tabular, subChildCommand);
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
    Path conf = cliParser.getPath(CONF_VARIABLES_PATH_PROPERTY);
    if (conf == null) {
      if (tabular.isProjectRun()) {
        conf = tabular.getProjectConfigurationFile().getVariablesPath();
      } else {
        conf = tabular.getEnvVariables().getUserConfigurationFile();
      }
    }
    return conf;
  }

}

