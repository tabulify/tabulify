package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static net.bytle.db.cli.DbDatabase.BYTLE_DB_DATABASES_STORE;
import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;
import static net.bytle.db.cli.Words.INFO_COMMAND;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbDatabaseInfo {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String NAME_OR_GLOB = "name|glob";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Print database information in a form fashion.";

        String footer = "Example:  To output information about the database `name`:\n" +
                "    db " + Words.DATABASE_COMMAND + " " + INFO_COMMAND + " name";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);

        cliCommand.argOf(NAME_OR_GLOB)
                .setDescription("one or more database name or glob patterns")
                .setDefaultValue("*");

        cliCommand.optionOf(DbDatabase.STORAGE_PATH)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatabase.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_STORE);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Retrieve
        List<String> names = cliParser.getStrings(NAME_OR_GLOB);
        final List<Database> databases = databasesStore.getDatabases(names);

        //Print

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        for (Database database:databases) {
            database.printDatabaseInformation();
        }
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);

        LOGGER.info("Bye !");


    }


}
