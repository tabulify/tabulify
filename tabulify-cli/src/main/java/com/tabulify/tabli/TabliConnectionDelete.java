package com.tabulify.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionVault;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;

import java.util.Collections;
import java.util.List;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;


/**
 * <p>
 */
public class TabliConnectionDelete {


  private static final String NAME_OR_GLOB_PATTERN = "name|glob";


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    /**
     * Define the command
     */
    childCommand.setDescription("Delete a connection from the connection vault");

    childCommand.addArg(NAME_OR_GLOB_PATTERN)
      .setDescription("a connection name or a glob pattern")
      .setMandatory(true);

    childCommand.addFlag(TabliWords.NOT_STRICT_FLAG)
      .setDescription("If the removed connection does not exist, the command will not exit with a failure code.");

    CliParser cliParser = childCommand.parse();

    DataPath feedbackDataPath = tabular.getMemoryDataStore().getDataPath("deletedConnection")
      .setDescription("The connection(s) deleted")
      .getOrCreateRelationDef()
      .addColumn("connection")
      .getDataPath();

    final List<String> names = cliParser.getStrings(NAME_OR_GLOB_PATTERN);
    for (String name : names) {
      final List<Connection> connections = tabular.selectConnections(name);
      if (connections.isEmpty()) {
        tabular.warningOrTerminateIfStrict("There is no connection called (" + name + ")");
      } else {
        LOGGER_TABLI.fine(connections.size() + " connections were found");
      }

      try(
        InsertStream insertStream = feedbackDataPath.getInsertStream();
        ConnectionVault connectionVault = ConnectionVault.create(tabular, tabular.getConnectionVaultPath())
      ) {
        for (Connection connection : connections) {
          connectionVault.deleteConnection(connection.getName());
          connectionVault.flush();
          LOGGER_TABLI.fine("The connection (" + connection.getName() + ") was removed");
          insertStream.insert(connection);
        }
      }

    }

    return Collections.singletonList(feedbackDataPath);
  }
}
