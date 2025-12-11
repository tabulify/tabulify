package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreStatic;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAttribute;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * See also {@link SqlDataStoreStatic}
 */
public class TabulConnectionType {

  protected static final String CONNECTION_NAMES = "(ConnectionNamePattern)...";

  protected static final String SUPPORTED_ONLY = "--supported-only";
  protected static final String ALL_COLUMNS = "--all-columns";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    String description = "Print the supported data type for a connection";

    // Add information about the command
    childCommand
      .setDescription(description)
      .addExample(
        "To output type information about the connection `name`:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " name",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To output type information about all the connection with `sql` in their name:" +
          CliUsage.getFullChainOfCommand(childCommand) + " *sql*");

    childCommand.addArg(CONNECTION_NAMES)
      .setDescription("one or more connection name (in glob format)")
      .setDefaultValue("*");

    childCommand.addFlag(TabulWords.NOT_STRICT_EXECUTION_FLAG)
      .setDescription("If there is no connection found, no errors will be emitted");
    childCommand.addFlag(SUPPORTED_ONLY)
      .setDescription("If this flag is present, the known but not-supported type name are also shown")
      .setDefaultValue(false);
    childCommand.addFlag(ALL_COLUMNS)
      .setDescription("If this flag is present, all type columns are shown")
      .setDefaultValue(false);

    CliParser cliParser = childCommand.parse();


    // Retrieve
    List<String> connectionNames = cliParser.getStrings(CONNECTION_NAMES);
    final List<Connection> connections = tabular.selectConnections(connectionNames.toArray(new String[0]));

    List<DataPath> feedbackDataPaths = new ArrayList<>();
    if (connections.isEmpty()) {
      tabular.warningOrTerminateIfStrict("No connection was found with the names (" + String.join(", ", connectionNames) + ")");
      return List.of();
    }

    Boolean supportedOnly = cliParser.getBoolean(SUPPORTED_ONLY);
    Boolean allColumns = cliParser.getBoolean(ALL_COLUMNS);

    List<SqlDataTypeAttribute> sqlDataTypeAttributes;
    if (!allColumns) {
      sqlDataTypeAttributes = List.of(
        SqlDataTypeAttribute.NAME,
        SqlDataTypeAttribute.ALIASES,
        SqlDataTypeAttribute.ANSI_TYPE,
        SqlDataTypeAttribute.MAX_PRECISION,
        SqlDataTypeAttribute.SUPPORTED,
        SqlDataTypeAttribute.DESCRIPTION
      );
    } else {
      sqlDataTypeAttributes = List.of(
        SqlDataTypeAttribute.NAME,
        SqlDataTypeAttribute.ALIASES,
        SqlDataTypeAttribute.JDBC_CODE,
        SqlDataTypeAttribute.JDBC_NAME,
        SqlDataTypeAttribute.ANSI_TYPE,
        SqlDataTypeAttribute.CLASS,
        /**
         * Number only attribute
         */
        SqlDataTypeAttribute.MAX_PRECISION,
        SqlDataTypeAttribute.MIN_SCALE,
        SqlDataTypeAttribute.MAX_SCALE,
        SqlDataTypeAttribute.AUTO_INCREMENT,
        SqlDataTypeAttribute.UNSIGNED,
        SqlDataTypeAttribute.FIXED_PRECISION_SCALE,
        /**
         * Create Parameters
         */
        SqlDataTypeAttribute.PARAMETERS,
        /**
         * Supported or not
         */
        SqlDataTypeAttribute.SUPPORTED,
        SqlDataTypeAttribute.DESCRIPTION
      );
    }
    /**
     * Connection Object
     */
    /**
     * Database Metadata Object
     */
    for (Connection connection : connections) {


      RelationDef feedbackDataDef = tabular.getMemoryConnection()
        .getDataPath(connection.getName() + "_type")
        .setComment("Known Types for the connection (" + connection + ")")
        .createRelationDef();
      for (SqlDataTypeAttribute sqlDataTypeAttribute : sqlDataTypeAttributes) {
        feedbackDataDef.addColumn(sqlDataTypeAttribute.toKeyNormalizer().toSqlCase());
      }

      feedbackDataPaths.add(feedbackDataDef.getDataPath());


      try (InsertStream insertStream = feedbackDataDef.getDataPath().getInsertStream()) {

        /**
         * Database Metadata Object
         */
        Set<SqlDataType<?>> sqlDataTypes = connection.getSqlDataTypes();
        sqlDataTypes
          .stream()
          .filter(s -> {
            // No children
            return s.getParent() == null;
          })
          .filter(s -> {
            if (!supportedOnly) {
              return true;
            }
            return s.getIsSupported();
          })
          .sorted().forEach(sqlDataType -> {
            try {

              List<Object> rowAttributes = new ArrayList<>();
              for (SqlDataTypeAttribute sqlDataTypeAttribute : sqlDataTypeAttributes) {
                rowAttributes.add(sqlDataType.getAttributeValue(sqlDataTypeAttribute));
              }
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

    return feedbackDataPaths;
  }
}
