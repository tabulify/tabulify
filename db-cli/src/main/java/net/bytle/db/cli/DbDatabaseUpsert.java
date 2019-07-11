package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;

import java.nio.file.Path;
import java.sql.Connection;

import static net.bytle.db.cli.DbDatabase.BYTLE_DB_DATABASES_STORE;
import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;
import static net.bytle.db.cli.Words.UPSERT_COMMAND;


/**
 * <p>
 */
public class DbDatabaseUpsert {

    protected static final String DRIVER = "driver";
    protected static final String URL = "url";
    protected static final String LOGIN = "login";
    protected static final String PASSPHRASE = "passphrase";
    protected static final String PASSWORD = "password";

    protected static final String STATEMENT = "statement";
    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATABASE_NAME = "name";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Update or insert a database";

        String footer = "Example:  To upsert the database information about the database `name`:\n" +
                "    db " + Words.DATABASE_COMMAND + " " + UPSERT_COMMAND + " "+ CliParser.PREFIX_LONG_OPTION + URL + " jdbc:sqlite//%TMP%/db.db name";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);

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

        cliCommand.optionOf(STORAGE_PATH)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatabase.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_STORE);


        cliCommand.optionOf(STATEMENT)
                .setShortName("s")
                .setDescription("A inline statement to be executed after a connection");

        cliCommand.getGroup("Database Properties")
                .addWordOf(URL)
                .addWordOf(LOGIN)
                .addWordOf(PASSWORD)
                .addWordOf(DRIVER)
                .addWordOf(PASSPHRASE)
                .addWordOf(STATEMENT)
                .addWordOf(STORAGE_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String databaseName = cliParser.getString(DATABASE_NAME);


        final String urlValue = cliParser.getString(URL);
        final String driverValue = cliParser.getString(DRIVER);
        final String userValue = cliParser.getString(LOGIN);
        final String pwdValue = cliParser.getString(PASSWORD);
        final String statementValue = cliParser.getString(STATEMENT);
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        final String passphrase = cliParser.getString(PASSPHRASE);

        if (pwdValue != null && passphrase == null) {
            LOGGER.severe("A passphrase is mandatory to store a password");
            System.exit(1);
        }

        Database database = Databases.of(databaseName)
                .setUrl(urlValue)
                .setDriver(driverValue)
                .setUser(userValue)
                .setPassword(pwdValue)
                .setStatement(statementValue);

        Connection connection = null;
        try {
            connection = database.getCurrentConnection();
        } catch (Exception e) {
            LOGGER.warning("We were unable to make a connection to the database " + databaseName);
        }
        if (connection != null) {
            System.out.println("Connection pinged");
            database.close();
        }

        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue)
                .setPassphrase(passphrase);
        if (databasesStore.getDatabases(databaseName).size() == 0) {
            if (database.getUrl()==null){
                LOGGER.severe("The database doesn't exist. An Url should be then specified");
                System.exit(1);
            }
        } else {
            LOGGER.info("The database exist already");
        }
        databasesStore.save(database);
        LOGGER.info("The database (" + databaseName + ") was saved.");


        LOGGER.info("Bye !");


    }


}
