package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.ConfVault;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.log.Log;
import net.bytle.log.Logs;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;
import static com.tabulify.tabli.TabliWords.*;


public class TabliConnection {


  protected static final String URI_PROPERTY = "--uri";
  private static final String CONNECTION_NAME = "name";

  private static final Log LOGGER = Logs.createFromClazz(TabliConnectionAdd.class);

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("Management of the Datastore Vault",
      "",
      "(Location: " + tabular.getConfPath() + ")"
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
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
    }
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
          throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
      }

    }
    return feedbackDataPaths;
  }

  /**
   * Common code on add and upsert command
   */
  static void addOrUpsertConnection(Tabular tabular, CliCommand childCommand, String command) {

    childCommand.addArg(CONNECTION_NAME)
      .setDescription("the connection name")
      .setMandatory(true);

    String uriDescForUpsertOnly = "";
    String uriDesc = "The system connection string (a JDBC Uri for a database or a file system URI).";
    switch (command) {
      case ADD_COMMAND:
        childCommand.addArg(TabliConnectionAdd.URI_ARGUMENT)
          .setDescription(uriDesc + uriDescForUpsertOnly)
          .setMandatory(true);
        break;
      case UPSERT_COMMAND:
      default:
        uriDescForUpsertOnly = "\nIf the connection doesn't exist, this option is mandatory.";
        childCommand.addProperty(URI_PROPERTY)
          .setDescription(uriDesc + uriDescForUpsertOnly)
          .setMandatory(true);
        break;
    }


    childCommand.addProperty(TabliConnectionAdd.USER_PROPERTY)
      .setShortName("-u")
      .setDescription("The user name (ie the login name)")
      .setValueName("user");

    childCommand.addProperty(TABLI_ATTRIBUTE_OPTION)
      .setDescription("A tabulify connection attribute")
      .setValueName("key=value");

    childCommand.addProperty(NATIVE_ATTRIBUTE)
      .setDescription("A native connection attribute (Example: JDBC driver property")
      .setValueName("key=value");


    childCommand.addProperty(TabliConnectionAdd.PASSWORD_PROPERTY)
      .setShortName("-p")
      .setValueName("password")
      .setDescription("The user password (Clear password is encrypted only if a passphrase is provided)");


    childCommand.addProperty(TabliConnectionAdd.DRIVER_PROPERTY)
      .setShortName("-d")
      .setDescription("The jdbc driver (JDBC URL only)")
      .setValueName("tld.package.driverClass");


    childCommand.getGroup("Connection Properties")
      .addWordOf(TabliConnectionAdd.URI_ARGUMENT)
      .addWordOf(TabliConnectionAdd.USER_PROPERTY)
      .addWordOf(TabliConnectionAdd.PASSWORD_PROPERTY)
      .addWordOf(TabliConnectionAdd.DRIVER_PROPERTY)
      .addWordOf(TABLI_ATTRIBUTE_OPTION)
      .addWordOf(NATIVE_ATTRIBUTE)
    ;

    // Args control
    CliParser cliParser = childCommand.parse();

    final String connectionName = cliParser.getString(CONNECTION_NAME);

    // Main
    Path connectionVaultPath = tabular.getConfPath();
    Connection connection;
    ConfVault confVault = ConfVault.createFromPath(connectionVaultPath, tabular);

    connection = confVault.getConnection(connectionName);
    String uriValue;
    switch (command) {
      case ADD_COMMAND:
        uriValue = cliParser.getString(TabliConnectionAdd.URI_ARGUMENT);
        break;
      case UPSERT_COMMAND:
      default:
        uriValue = cliParser.getString(URI_PROPERTY);
        break;
    }

    // origin must be conf, otherwise it will not be in the flush
    Origin confOrigin = Origin.CONF;

    if (connection == null) {
      connection = Connection.createConnectionFromProviderOrDefault(tabular, connectionName, uriValue);
      LOGGER.info("The connection (" + connectionName + ") was created");
    } else {
      if (command.equals(ADD_COMMAND)) {
        throw new RuntimeException("The connection (" + connectionName + ") exist already. It can't then be added, use the upsert command instead. (Connection vault location: " + connectionVaultPath + ")");
      }
      LOGGER.info("The connection (" + connectionName + ") was found, updating");
      if (uriValue != null && !connection.getUri().toString().equals(uriValue)) {
        // a new uri, means a new connection (normally only for a new scheme but yeah)
        Connection newConnection = Connection.createConnectionFromProviderOrDefault(tabular, connectionName, uriValue);
        for (Attribute attribute : connection.getAttributes()) {
          // only conf origin
          // don't modify the URI attribute as it's the new one
          if (attribute.getOrigin() != confOrigin || attribute.getAttributeMetadata() == ConnectionAttributeEnumBase.URI) {
            continue;
          }
          newConnection.addAttribute((ConnectionAttributeEnum) attribute.getAttributeMetadata(), attribute.getValueOrNull(), confOrigin, tabular.getVault());
        }
        for (Map.Entry<String, Attribute> nativeAttribute : connection.getNativeDriverAttributes().entrySet()) {
          if (nativeAttribute.getValue().getOrigin() != confOrigin) {
            continue;
          }
          newConnection.addNativeAttribute(nativeAttribute.getKey(), (String) nativeAttribute.getValue().getValueOrNull(), confOrigin, tabular.getVault());
        }
        connection = newConnection;
      }
    }


    final String userValue = cliParser.getString(TabliConnectionAdd.USER_PROPERTY);


    if (userValue != null) {
      connection.addAttribute(ConnectionAttributeEnumBase.USER, userValue, confOrigin, tabular.getVault());
    }
    String pwdValue = cliParser.getString(TabliConnectionAdd.PASSWORD_PROPERTY);
    if (pwdValue != null) {
      pwdValue = tabular.getVault().encryptIfPossible(pwdValue);
      connection.addAttribute(ConnectionAttributeEnumBase.PASSWORD, pwdValue, confOrigin, tabular.getVault());
    }

    final String driverValue = cliParser.getString(TabliConnectionAdd.DRIVER_PROPERTY);
    if (driverValue != null) {
      connection.addAttribute(ConnectionAttributeEnumBase.DRIVER, driverValue, confOrigin, tabular.getVault());
    }

    Map<String, String> tabliAttributes = cliParser.getProperties(TABLI_ATTRIBUTE_OPTION);
    for (Map.Entry<String, String> tabliAttribute : tabliAttributes.entrySet()) {
      connection.addAttribute(KeyNormalizer.createSafe(tabliAttribute.getKey()), tabliAttribute.getValue(), confOrigin, tabular.getVault());
    }

    Map<String, String> nativeAttributes = cliParser.getProperties(NATIVE_ATTRIBUTE);
    for (Map.Entry<String, String> nativeAttribute : nativeAttributes.entrySet()) {
      connection.addNativeAttribute(nativeAttribute.getKey(), nativeAttribute.getValue(), confOrigin, tabular.getVault());
    }

    confVault
      .addConnection(connection)
      .flush();
    LOGGER.info("The connection (" + connectionName + ") was saved into the connection vault (" + connectionVaultPath + ")");


    // Ping test ?
    try {
      connection.getDataSystem();
      LOGGER.info("Connection pinged");
    } catch (Exception e) {
      String msg = "We were unable to make a connection to the connection" + connectionName;
      if (!tabular.isStrict()) {
        LOGGER.warning(msg);
      } else {
        throw new RuntimeException(msg);
      }
    }
  }
}
