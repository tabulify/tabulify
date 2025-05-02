package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreStatic;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * See also {@link SqlDataStoreStatic}
 */
public class TabliConnectionInfo {

  protected static final String CONNECTION_NAMES = "(NamePattern)...";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    String description = "Print connection attributes in a form fashion.";

    // Add information about the command
    childCommand
      .setDescription(description)
      .addExample(
        "To output information about the connection `name`:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " name",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To output information about all the connection with `sql` in their name:" +
          CliUsage.getFullChainOfCommand(childCommand) + " sql*");

    childCommand.addArg(CONNECTION_NAMES)
      .setDescription("one or more connection name")
      .setDefaultValue("*");

    childCommand.addFlag(TabliWords.NOT_STRICT_FLAG)
      .setDescription("If there is no connection found, no errors will be emitted");

    CliParser cliParser = childCommand.parse();


    // Retrieve
    List<String> connectionNames = cliParser.getStrings(CONNECTION_NAMES);
    final List<Connection> connections = tabular.selectConnections(connectionNames.toArray(new String[0]));

    List<DataPath> feedbackDataPaths = new ArrayList<>();
    if (connections.isEmpty()) {
      tabular.warningOrTerminateIfStrict("No connection was found with the name (" + connectionNames + ")");
    } else {


      /**
       * Connection Object
       */
      /**
       * Database Metadata Object
       */
      for (Connection connection : connections) {

        RelationDef feedbackDataDef = tabular.getMemoryDataStore()
          .getDataPath(connection.getName() + "_info")
          .setDescription("Information about the connection (" + connection + ")")
          .createRelationDef()
          .addColumn("Attribute")// attribute and not properties because the product is called `tabulify`
          .addColumn("Value")
          .addColumn("Description");
        feedbackDataPaths.add(feedbackDataDef.getDataPath());
        try (InsertStream insertStream = feedbackDataDef.getDataPath().getInsertStream()) {

          /**
           * Connection Object
           */
          List<Object> rowConnection = new ArrayList<>();
          try {
            rowConnection.add("Working Path");
            rowConnection.add(connection.getCurrentDataPath().getAbsolutePath());
            rowConnection.add("Schema for database, directory for file system");
            insertStream.insert(rowConnection);
          } catch (Exception e) {
            // not implemented (smtp for instance)
          }

          /**
           * Database Metadata Object
           */
          connection.getAttributes().stream().sorted().forEach(attribute -> {
            try {

              List<Object> rowAttributes = new ArrayList<>();
              rowAttributes.add(tabular.toPublicName(attribute));
              rowAttributes.add(attribute.getValueOrDefaultOrNull());
              rowAttributes.add(attribute.getAttributeMetadata().getDescription());
              insertStream.insert(rowAttributes);

            } catch (Exception e) {
              // If we can't connect for instance
              if (!(e.getCause() instanceof SQLException)) {
                throw e;
              }
            }
          });
        }
      }
    }

    return feedbackDataPaths;
  }
}
