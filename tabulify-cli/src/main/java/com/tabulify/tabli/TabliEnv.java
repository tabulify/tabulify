package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;

import java.util.List;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;
import static com.tabulify.tabli.TabliWords.*;


public class TabliEnv {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.addChildCommand(SET_COMMAND)
      .setDescription("Set a global attribute");
    childCommand.addChildCommand(LIST_COMMAND)
      .setDescription("List the global attributes");
    childCommand.addChildCommand(DELETE_COMMAND)
      .setDescription("Delete a global attribute");
    childCommand.addChildCommand(DIAGNOSTIC_COMMAND)
      .setDescription("Print env diagnostic information");


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
            feedbackDataPaths = TabliEnvAttributeList.run(tabular, subChildCommand);
            break;
          case SET_COMMAND:
            feedbackDataPaths = TabliEnvAttributeSet.run(tabular, subChildCommand);
            break;
          case DELETE_COMMAND:
            feedbackDataPaths = TabliEnvAttributeDelete.run(tabular, subChildCommand);
            break;
          case TabliWords.DIAGNOSTIC_COMMAND:
            feedbackDataPaths = TabliDiagnostic.run(tabular, childCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }

    return feedbackDataPaths;
  }



}

