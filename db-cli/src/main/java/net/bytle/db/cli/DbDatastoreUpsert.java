package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.DataStore;
import net.bytle.log.Log;
import net.bytle.type.Strings;

import java.nio.file.Path;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.UPSERT_COMMAND;


/**
 * <p>
 */
public class DbDatastoreUpsert {

  protected static final String DRIVER = "driver";
  protected static final String URL = "url";
  protected static final String LOGIN = "login";
  protected static final String PASSPHRASE = "passphrase";
  protected static final String PASSWORD = "password";

  private static final Log LOGGER = Db.LOGGER_DB_CLI;
  private static final String DATABASE_NAME = "name";


  public static void run(CliCommand cliCommand, String[] args) {


    // Define the command and its arguments
    cliCommand
      .setDescription("Update or insert a datastore")
      .addExample(Strings.multiline(
        "To upsert the datastore information about the datastore called `db`",
        "    db " + Words.DATASTORE_COMMAND + " " + UPSERT_COMMAND + " " + CliParser.PREFIX_LONG_OPTION + URL + " jdbc:sqlite//%TMP%/db.db db"
      ));

    cliCommand.optionOf(DATASTORE_VAULT_PATH)
      .setMandatory(true);

    cliCommand.argOf(DATABASE_NAME)
      .setDescription("the database name")
      .setMandatory(true);

    cliCommand.optionOf(URL)
      .setShortName("u")
      .setDescription("The database url (if the database doesn't exist, this options is mandatory)");

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
      .setDescription("The jdbc driver (for a jdbc connection)");


    cliCommand.getGroup("Database Properties")
      .addWordOf(URL)
      .addWordOf(LOGIN)
      .addWordOf(PASSWORD)
      .addWordOf(DRIVER)
      .addWordOf(PASSPHRASE)
      .addWordOf(DATASTORE_VAULT_PATH);

    // Args control
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final String datastoreName = cliParser.getString(DATABASE_NAME);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String passphrase = cliParser.getString(PASSPHRASE);
    final String urlValue = cliParser.getString(URL);
    final String userValue = cliParser.getString(LOGIN);
    final String pwdValue = cliParser.getString(PASSWORD);
    final String driverValue = cliParser.getString(DRIVER);

    if (pwdValue != null && passphrase == null) {
      LOGGER.severe("A passphrase is mandatory to store a password");
      System.exit(1);
    }

    // Main
    try (DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue)
      ) {
      DataStore dataStore = datastoreVault.getDataStore(datastoreName);
      if (dataStore == null) {
        dataStore = DataStore.of(datastoreName, urlValue);
        datastoreVault.add(dataStore);
        LOGGER.info("The datastore (" + datastoreName + ") was added");
      } else {
        datastoreVault.update(dataStore);
        LOGGER.info("The datastore (" + datastoreName + ") was updated.");

      }
      LOGGER.info("Bye !");
    }

  }


}
