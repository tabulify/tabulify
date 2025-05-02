package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.conf.ConnectionVault;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeBase;
import com.tabulify.connection.ConnectionOrigin;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliConnectionAdd.*;


/**
 * <p>
 */
public class TabliConnectionUpsert {


  protected static final String URL_PROPERTY = "--url";


  private static final String CONNECTION_NAME = "name";


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Define the command and its arguments
    childCommand
      .setDescription("Update or insert a connection")
      .addExample(
        "To upsert the information of the connection called `db`",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + URL_PROPERTY + " jdbc:sqlite//%TMP%/db.db db",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(CONNECTION_NAME)
      .setDescription("the connection name")
      .setMandatory(true);

    childCommand.addProperty(URL_PROPERTY)
      .setDescription("The connection url (if the connection doesn't exist, this options is mandatory)")
      .setMandatory(true);

    childCommand.addProperty(USER_PROPERTY)
      .setShortName("-u")
      .setDescription("The login (ie user)");


    childCommand.addProperty(PASSWORD_PROPERTY)
      .setShortName("-p")
      .setDescription("The user password");


    childCommand.addProperty(DRIVER_PROPERTY)
      .setShortName("-d")
      .setDescription("The jdbc driver (for a jdbc connection)");


    childCommand.getGroup("Connection Properties")
      .addWordOf(URL_PROPERTY)
      .addWordOf(USER_PROPERTY)
      .addWordOf(PASSWORD_PROPERTY)
      .addWordOf(DRIVER_PROPERTY);

    // Args control
    CliParser cliParser = childCommand.parse();

    final String connectionName = cliParser.getString(CONNECTION_NAME);
    final String urlValue = cliParser.getString(URL_PROPERTY);
    final String userValue = cliParser.getString(USER_PROPERTY);
    final String pwdValue = cliParser.getString(PASSWORD_PROPERTY);
    final String driverValue = cliParser.getString(DRIVER_PROPERTY);


    // Main
    Path connectionVaultPath = tabular.getConfPath();
    try (ConnectionVault connectionVault = ConnectionVault.create(tabular, connectionVaultPath)) {
      Connection connection = connectionVault.getConnection(connectionName);
      if (connection == null) {
        connection = Connection.createConnectionFromProviderOrDefault(tabular, connectionName, urlValue)
          .setOrigin(ConnectionOrigin.CONF);
        connectionVault.put(connection);
        System.out.println("The connection (" + connectionName + ") didn't exist and was created");
      } else {
        System.out.println("The connection (" + connectionName + ") exist already.");
        if (!connection.getUriAsString().equals(urlValue)) {
          connection = Connection.createConnectionFromProviderOrDefault(tabular, connectionName, urlValue)
            .setDescription((String) connection.getDescription().getValueOrDefaultOrNull())
            .setOrigin(connection.getOrigin())
            .setAttributes(connection.getAttributes()
              .stream()
              .filter(v -> v.getAttributeMetadata() != ConnectionAttributeBase.URI)
              .collect(Collectors.toSet()))
            .setPassword(pwdValue)
            .setUser((String) connection.getUser().getValueOrDefaultOrNull());
          connectionVault.put(connection);
        }
      }
      connection
        .setUser(userValue)
        .setPassword(pwdValue);
      if (driverValue != null) {
        connection.addAttribute(ConnectionAttributeBase.DRIVER, driverValue);
      }
      connectionVault.flush();
    }
    System.out.println("The connection (" + connectionName + ") was upsert-ed in (" + connectionVaultPath + ")");

    return new ArrayList<>();
  }

}


