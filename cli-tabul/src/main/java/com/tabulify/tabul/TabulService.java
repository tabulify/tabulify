package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;
import static com.tabulify.tabul.TabulWords.*;


public class TabulService {

  protected static final String SERVICE_NAMES = "(NamePattern)...";


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("Management of the Services (docker, ...)");

    childCommand.addChildCommand(TabulWords.START_COMMAND)
      .setDescription("Start a service (Optionally creating it if non-existent)");
    childCommand.addChildCommand(TabulWords.STOP_COMMAND)
      .setDescription("Stop a service");
    childCommand.addChildCommand(DROP_COMMAND)
      .setDescription("Drop a service (In the service system, not in the configuration vault)");
    childCommand.addChildCommand(TabulWords.PING_COMMAND)
      .setDescription("Ping a service (with a connection name)");
    childCommand.addChildCommand(INFO_COMMAND)
      .setDescription("Show the service attributes");
    childCommand.addChildCommand(LIST_COMMAND)
      .setDescription("List the available services");

    CliParser cliParser = childCommand.parse();
    List<DataPath> feedbackDataPaths = new ArrayList<>();

    List<CliCommand> commands = cliParser.getFoundedChildCommands();
    if (commands.isEmpty()) {
      throw new IllegalCommandException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
    }
    for (CliCommand subChildCommand : commands) {
      LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
      switch (subChildCommand.getName()) {
        case START_COMMAND:
          feedbackDataPaths = TabulServiceStart.run(tabular, subChildCommand);
          break;
        case STOP_COMMAND:
          feedbackDataPaths = TabulServiceStop.run(tabular, subChildCommand);
          break;
        case DROP_COMMAND:
          feedbackDataPaths = TabulServiceDrop.run(tabular, subChildCommand);
          break;
        case LIST_COMMAND:
          feedbackDataPaths = TabulServiceList.run(tabular, subChildCommand);
          break;
        case INFO_COMMAND:
          feedbackDataPaths = TabulServiceInfo.run(tabular, subChildCommand);
          break;
        case PING_COMMAND:
          feedbackDataPaths = TabulConnectionPing.run(tabular, subChildCommand);
          break;
        default:
          throw new IllegalCommandException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
      }

    }
    return feedbackDataPaths;
  }


}
