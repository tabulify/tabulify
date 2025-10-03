package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;

import java.util.List;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;
import static com.tabulify.tabul.TabulWords.*;


public class TabulEnv {

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
        LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case LIST_COMMAND:
            feedbackDataPaths = TabulEnvAttributeList.run(tabular, subChildCommand);
            break;
          case SET_COMMAND:
            feedbackDataPaths = TabulEnvAttributeSet.run(tabular, subChildCommand);
            break;
          case DELETE_COMMAND:
            feedbackDataPaths = TabulEnvAttributeDelete.run(tabular, subChildCommand);
            break;
          case TabulWords.DIAGNOSTIC_COMMAND:
            feedbackDataPaths = TabulDiagnostic.run(tabular, childCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }

    return feedbackDataPaths;
  }



}

