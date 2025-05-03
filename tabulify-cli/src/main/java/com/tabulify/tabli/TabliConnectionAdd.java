package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.conf.ConfVault;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.crypto.Protector;
import net.bytle.log.Log;
import net.bytle.log.Logs;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tabulify.tabli.TabliWords.ATTRIBUTE_PROPERTY;


/**
 * <p>
 */
public class TabliConnectionAdd {

  protected static final String DRIVER_PROPERTY = "--driver";
  protected static final String URI_ARGUMENT = "uri";
  protected static final String USER_PROPERTY = "--user";
  protected static final String PASSWORD_PROPERTY = "--password";


  private static final String CONNECTION_NAME = "name";

  private static final Log LOGGER = Logs.createFromClazz(TabliConnectionAdd.class);

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the parser
    childCommand
      .setDescription("Add a connection")
      .addExample("To add the connection `name` with the URL `url`:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " name url",
        CliUsage.CODE_BLOCK
      );

    childCommand.addArg(CONNECTION_NAME)
      .setDescription("the connection name")
      .setMandatory(true);

    childCommand.addArg(URI_ARGUMENT)
      .setDescription("The system connection string (a JDBC Url for a database or a file system URL)")
      .setMandatory(true);

    childCommand.addProperty(USER_PROPERTY)
      .setShortName("-u")
      .setDescription("The user name (ie the login name)")
      .setValueName("user");

    childCommand.addProperty(ATTRIBUTE_PROPERTY)
      .setDescription("A attribute (known also as connection properties)")
      .setValueName("key=value");


    childCommand.addProperty(PASSWORD_PROPERTY)
      .setShortName("-p")
      .setValueName("password")
      .setDescription("The user password (A passphrase is then mandatory because password should be encrypted)");


    childCommand.addProperty(DRIVER_PROPERTY)
      .setShortName("-d")
      .setDescription("The jdbc driver (JDBC URL only)")
      .setValueName("tld.package.driverClass");


    childCommand.getGroup("Connection Properties")
      .addWordOf(URI_ARGUMENT)
      .addWordOf(USER_PROPERTY)
      .addWordOf(PASSWORD_PROPERTY)
      .addWordOf(DRIVER_PROPERTY)
      .addWordOf(ATTRIBUTE_PROPERTY);

    // Args control
    CliParser cliParser = childCommand.parse();
    final String connectionName = cliParser.getString(CONNECTION_NAME);
    final String urlValue = cliParser.getString(URI_ARGUMENT);
    final String driverValue = cliParser.getString(DRIVER_PROPERTY);
    final String userValue = cliParser.getString(USER_PROPERTY);
    String pwdValue = cliParser.getString(PASSWORD_PROPERTY);


    // Main
    Path connectionVaultPath = tabular.getConfPath();
    Connection connection;
    ConfVault confVault = ConfVault.createFromPath(connectionVaultPath, tabular);

    connection = confVault.getConnection(connectionName);

    if (connection != null) {

      throw new RuntimeException("The connection (" + connectionName + ") exist already. It can't then be added (Connection vault location: " + connectionVaultPath + ")");

    }

    connection = Connection.createConnectionFromProviderOrDefault(tabular, connectionName, urlValue);
    // origin must be conf, otherwise it will not be in the flush
    Origin origin = Origin.CONF;
    connection.addAttribute(ConnectionAttributeEnumBase.USER, userValue, origin);
    Protector protector = tabular.getProtector();
    if (protector != null && !pwdValue.startsWith(Vault.VAULT_PREFIX)) {
      pwdValue = tabular.getVault().encrypt(pwdValue);
    }
    connection.addAttribute(ConnectionAttributeEnumBase.PASSWORD, pwdValue, origin);
    connection.addAttribute(ConnectionAttributeEnumBase.DRIVER, driverValue, origin);

    for (Map.Entry<String, String> property : cliParser.getProperties(ATTRIBUTE_PROPERTY).entrySet()) {
      connection.addAttribute(KeyNormalizer.create(property.getKey()), property.getValue(), origin);
    }

    confVault
      .addConnection(connection)
      .flush();
    System.out.println("The connection (" + connectionName + ") was saved into the connection vault (" + connectionVaultPath + ")");


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


    return new ArrayList<>();

  }

}


