package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.DataStore;
import net.bytle.db.jdbc.JdbcDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static net.bytle.db.cli.Words.ADD_COMMAND;
import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


/**
 * <p>
 */
public class DbDatastoreAdd {

  protected static final String DRIVER = "driver";
  protected static final String URL = "url";
  protected static final String LOGIN = "login";
  protected static final String PASSPHRASE = "passphrase";
  protected static final String PASSWORD = "password";


  private static final String DATASTORE_NAME = "name";

  private static final Logger LOGGER = LoggerFactory.getLogger(DbDatastoreAdd.class);
  ;

  public static void run(CliCommand cliCommand, String[] args) {


    String description = "Add a datastore";

    String footer = "Example: To add the datastore information about the datastore `name`:\n" +
      "    db " + Words.DATASTORE_COMMAND + " " + ADD_COMMAND + " name";

    // Create the parser
    cliCommand
      .setDescription(description)
      .setFooter(footer);

    cliCommand.optionOf(DATASTORE_VAULT_PATH)
      .setMandatory(true);

    cliCommand.argOf(DATASTORE_NAME)
      .setDescription("the datastore name")
      .setMandatory(true);

    cliCommand.argOf(URL)
      .setShortName("u")
      .setDescription("The datastore connection string (a JDBC Url for a datastore or a file system URL)")
      .setMandatory(true);

    cliCommand.optionOf(LOGIN)
      .setShortName("l")
      .setDescription("The login (ie user)");


    cliCommand.optionOf(PASSWORD)
      .setShortName("p")
      .setDescription("The user password");

    cliCommand.optionOf(PASSPHRASE)
      .setShortName("pp")
      .setDescription("A passphrase (master password) to encrypt the password")
      .setEnvName("BYTLE_DB_PASSPHRASE");

    cliCommand.optionOf(DRIVER)
      .setShortName("d")
      .setDescription("The jdbc driver (JDBC URL only)");


    cliCommand.getGroup("Datastore Properties")
      .addWordOf(URL)
      .addWordOf(LOGIN)
      .addWordOf(PASSWORD)
      .addWordOf(DRIVER)
      .addWordOf(PASSPHRASE)
      .addWordOf(DATASTORE_VAULT_PATH);

    // Args control
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final String datastoreName = cliParser.getString(DATASTORE_NAME);
    final String urlValue = cliParser.getString(URL);
    final String driverValue = cliParser.getString(DRIVER);
    final String userValue = cliParser.getString(LOGIN);
    final String pwdValue = cliParser.getString(PASSWORD);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String passphrase = cliParser.getString(PASSPHRASE);

    if (pwdValue != null && passphrase == null) {
      LOGGER.error("A passphrase is mandatory to store a password");
      System.exit(1);
    }

    // Main
    try (DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue,passphrase)) {

      DataStore dataStore = datastoreVault.getDataStore(datastoreName);

      if (dataStore != null) {
        LOGGER.error("The datastore ({}) exist already. It can't then be added (Data store vault location: {}). ", datastoreName, storagePathValue);
        System.exit(1);
      } else {

        dataStore = DataStore.of(datastoreName, urlValue)
          .setUser(userValue)
          .setPassword(pwdValue)
          .addProperty(JdbcDataStore.DRIVER_PROPERTY_KEY, driverValue);
      }
      datastoreVault.add(dataStore);
      LOGGER.info("The datastore ({}) was saved.", datastoreName);

      // Ping test ?
      try {
        dataStore.getDataSystem();
      } catch (Exception e) {
        LOGGER.warn("We were unable to make a connection to the datastore {}", datastoreName);
      }
      System.out.println("Connection pinged");
      dataStore.close();

      LOGGER.info("Bye !");
    }

  }


}
