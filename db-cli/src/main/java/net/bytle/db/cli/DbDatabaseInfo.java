package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;

import java.util.logging.Level;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.INFO_COMMAND;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbDatabaseInfo {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final String DATABASE_NAME = "name";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Print database information.";

        String footer = "Example:  To output information about the database `name`:\n" +
                "    db " + Words.DATABASE_COMMAND + " " + INFO_COMMAND + " name";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);

        cliCommand.argOf(DATABASE_NAME)
                .setDescription("the database name")
                .setDefaultValue(Db.CLI_DATABASE_NAME_TARGET)
                .setMandatory(true);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String databaseName = cliParser.getString(DATABASE_NAME);
        String urlPropertyKey = "db." + databaseName + ".url";
        String driverPropertyKey = "db." + databaseName + ".driver";


        final String urlValue = cliParser.getString(urlPropertyKey);
        if (urlValue == null) {
            System.err.println("Unable to find the url property (" + urlPropertyKey + ") for the database (" + databaseName + ")");
            cliCommand.optionOf(urlPropertyKey).setDescription("The database URL");
            CliUsage.print(cliCommand);
            System.exit(1);
        }
        final String driverValue = cliParser.getString(driverPropertyKey);

        Database database = Databases.get(databaseName)
                .setUrl(urlValue)
                .setDriver(driverValue);

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        database.printDatabaseInformation();
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);

        LOGGER.info("Bye !");


    }


}
