package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;

import java.sql.Connection;

import static net.bytle.db.cli.Words.ADD_COMMAND;


/**
 * <p>
 */
public class DbDatabaseAdd {

    protected static final String driver = "driver";
    protected static final String url = "url";
    protected static final String login = "login";
    protected static final String master = "master";
    protected static final String password = "password";
    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATABASE_NAME = "name";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Add a database";

        String footer = "Example:  To add the database information about the database `name`:\n" +
                "    db " + Words.DATABASE_COMMAND + " " + ADD_COMMAND + " name";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);

        cliCommand.argOf(DATABASE_NAME)
                .setDescription("the database name")
                .setMandatory(true);


        cliCommand.argOf(url)
                .setShortName("u")
                .setDescription("The database url")
                .setMandatory(true);

        cliCommand.optionOf(login)
                .setShortName("l")
                .setDescription("The login (ie user)");


        cliCommand.optionOf(password)
                .setShortName("p")
                .setDescription("The user password");

        cliCommand.optionOf(master)
                .setShortName("m")
                .setDescription("A master password (passphrase) to encrypt the data");


        cliCommand.optionOf(driver)
                .setShortName("d")
                .setDescription("The jdbc driver");

        final String statement = "statement";
        cliCommand.optionOf(statement)
                .setShortName("s")
                .setDescription("A inline statement to be executed after a connection");

        cliCommand.getGroup("Database Properties")
                .addWordOf(url)
                .addWordOf(login)
                .addWordOf(password)
                .addWordOf(driver)
                .addWordOf(master)
                .addWordOf(statement);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String databaseName = cliParser.getString(DATABASE_NAME);


        final String urlValue = cliParser.getString(url);
        final String driverValue = cliParser.getString(driver);
        final String userValue = cliParser.getString(login);
        final String pwdValue = cliParser.getString(password);
        final String statementValue = cliParser.getString(statement);

        Database database = Databases.get(databaseName)
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
        Databases.save(database,cliParser.getString(master));
        LOGGER.info("The database ("+databaseName+") was saved.");
        LOGGER.info("Bye !");


    }


}
