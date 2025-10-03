package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;

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
    if (commands.size() == 0) {
      throw new IllegalArgumentException("A known command must be given");
    } else {

      for (CliCommand subChildCommand : commands) {
        LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case TabulWords.EXECUTE_COMMAND:
            feedbackDataPaths = TabulFlowExecute.run(tabular, subChildCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }
    return feedbackDataPaths;
  }

}
