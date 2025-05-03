package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.conf.ConfVault;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    ConfVault connectionVault = ConfVault.createFromPath(tabular.getConfPath(), tabular);
    try (
      InsertStream insertStream = feedbackDataPath.getInsertStream()
    ) {
      for (String name : names) {
        Set<String> deletedConnections = connectionVault.deleteConnection(name);
        if (deletedConnections.isEmpty()) {
          String msg = "The glob name (" + name + ") does not select any connections";
          if (tabular.isStrict()) {
            throw new RuntimeException(msg);
          } else {
            LOGGER_TABLI.fine(msg);
          }
          continue;
        }
        for (String deletedConnection : deletedConnections) {
          LOGGER_TABLI.fine("The connection (" + deletedConnection + ") was removed");
          insertStream.insert(deletedConnection);
        }

      }
      connectionVault.flush();
    }


    return Collections.singletonList(feedbackDataPath);
  }
}
