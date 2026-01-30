package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreStatic;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.type.KeyCase;
import com.tabulify.type.KeyNormalizer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * See also {@link SqlDataStoreStatic}
 */
public class TabulConnectionInfo {

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
          CliUsage.getFullChainOfCommand(childCommand) + " *sql*");

    childCommand.addArg(CONNECTION_NAMES)
      .setDescription("one or more connection name")
      .setDefaultValue("*");

    childCommand.addFlag(TabulWords.NOT_STRICT_EXECUTION_FLAG)
      .setDescription("If there is no connection found, no errors will be emitted");

    CliParser cliParser = childCommand.parse();

    // Retrieve
    List<String> connectionNames = cliParser.getStrings(CONNECTION_NAMES);

    /**
     * App bug: app is a {@link TabulWords.APP_COMMAND} command
     * and also an argument (ie name of a connection)
     * App is then seen as a command and not as an argument
     */
    if (cliParser.has("app") && connectionNames.get(0).equals("*")) {
      connectionNames = List.of("app");
    }

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

        RelationDef feedbackDataDef = tabular.getMemoryConnection()
          .getDataPath(connection.getName() + "_info")
          .setComment("Information about the connection (" + connection + ")")
          .createRelationDef()
          .addColumn("Attribute")// attribute and not properties because the product is called `tabulify`
          .addColumn("Value")
          .addColumn("Description");
        feedbackDataPaths.add(feedbackDataDef.getDataPath());

        // Upper snake to show that you can overwrite it with an operating system
        KeyCase snakeUpper = KeyCase.SNAKE_UPPER;
        try (InsertStream insertStream = feedbackDataDef.getDataPath().getInsertStream()) {

          /**
           * Database Metadata Object
           */
          connection.getAttributes().stream().sorted().forEach(attribute -> {
            try {

              List<Object> rowAttributes = new ArrayList<>();
              rowAttributes.add(KeyNormalizer.createSafe(attribute.getAttributeMetadata()).toCase(snakeUpper));
              rowAttributes.add(attribute.getPublicValue().orElse(null));
              rowAttributes.add(attribute.getAttributeMetadata().getDescription());
              insertStream.insert(rowAttributes);

            } catch (Exception e) {
              // For derived attribute, if we can't connect for instance
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
