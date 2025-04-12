package com.tabulify.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;
import static com.tabulify.tabli.TabliWords.*;


public class TabliConnection {




  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("Management of the Datastore Vault",
      "",
      "(Location: "+tabular.getConnectionVaultPath()+")"
    );

    childCommand.addChildCommand(TabliWords.ADD_COMMAND)
      .setDescription("Add a connection");
    childCommand.addChildCommand(TabliWords.UPSERT_COMMAND)
      .setDescription("Update or add a connection if it does't exist");
    childCommand.addChildCommand(TabliWords.LIST_COMMAND)
      .setDescription("List the connections");
    childCommand.addChildCommand(TabliWords.INFO_COMMAND)
      .setDescription("Show the attributes of a connection");
    childCommand.addChildCommand(DELETE_COMMAND)
      .setDescription("Delete a connection");
    childCommand.addChildCommand(TabliWords.PING_COMMAND)
      .setDescription("Ping a connection");

    CliParser cliParser = childCommand.parse();
    List<DataPath> feedbackDataPaths = new ArrayList<>();

    List<CliCommand> commands = cliParser.getFoundedChildCommands();
    if (commands.size() == 0) {
      throw new IllegalArgumentException("A known command must be given for the command ("+ CliUsage.getFullChainOfCommand(childCommand)+").");
    } else {
      for (CliCommand subChildCommand : commands) {
        LOGGER_TABLI.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case ADD_COMMAND:
            feedbackDataPaths = TabliConnectionAdd.run(tabular, subChildCommand);
            break;
          case UPSERT_COMMAND:
            feedbackDataPaths = TabliConnectionUpsert.run(tabular, subChildCommand);
            break;
          case LIST_COMMAND:
            feedbackDataPaths = TabliConnectionList.run(tabular, subChildCommand);
            break;
          case DELETE_COMMAND:
            feedbackDataPaths = TabliConnectionDelete.run(tabular, subChildCommand);
            break;
          case INFO_COMMAND:
            feedbackDataPaths = TabliConnectionInfo.run(tabular, subChildCommand);
            break;
          case PING_COMMAND:
            feedbackDataPaths = TabliConnectionPing.run(tabular, subChildCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command ("+ CliUsage.getFullChainOfCommand(childCommand)+")");
        }

      }
    }
    return feedbackDataPaths;
  }

}
