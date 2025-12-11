package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;

public class TabulFlow {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {
    childCommand.setDescription("The command on flow");

    childCommand.addChildCommand(TabulWords.EXECUTE_COMMAND)
      .setDescription("Execute one or more flow scripts");


    List<DataPath> feedbackDataPaths = new ArrayList<>();
    List<CliCommand> commands = childCommand.parse().getFoundedChildCommands();
    if (commands.isEmpty()) {
      throw new IllegalCommandException("A known command must be given");
    } else {

      for (CliCommand subChildCommand : commands) {
        LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case TabulWords.EXECUTE_COMMAND:
            feedbackDataPaths = TabulFlowExecute.run(tabular, subChildCommand);
            break;
          case "log":
            // Example of execution log command: https://www.nextflow.io/docs/latest/tracing.html#execution-log
          default:
            throw new IllegalCommandException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }
    return feedbackDataPaths;
  }

}
