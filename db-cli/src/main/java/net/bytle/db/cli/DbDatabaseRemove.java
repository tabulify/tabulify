package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.log.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.DbDatabase.BYTLE_DB_DATABASES_STORE;
import static net.bytle.db.cli.Words.DATABASE_STORE;


/**
 * <p>
 */
public class DbDatabaseRemove {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATABASE_PATTERN = "name";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Remove a database (metadata only)";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(DATABASE_PATTERN)
                .setDescription("the database name or a glob pattern")
                .setMandatory(true);

        cliCommand.optionOf(Words.DATABASE_STORE)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatabase.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_STORE);

        cliCommand.flagOf(Words.NO_STRICT)
                .setDescription("If the removed database does not exist, the command will not exit with a failure code.")
                .setDefaultValue(false);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);


        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        final Boolean noStrictMode = cliParser.getBoolean(Words.NO_STRICT);
        final List<String> names = cliParser.getStrings(DATABASE_PATTERN);
        for (String name : names) {
            final List<Database> databases = databasesStore.getDatabases(name);
            if (databases.size() == 0) {
                final String msg = "There is no database called (" + name + ")";
                if (noStrictMode){
                    LOGGER.warning(msg);
                } else {
                    LOGGER.severe(msg);
                    LOGGER.severe("If you don't want the process to continue without failure, you can set the no strict flag ("+CliParser.PREFIX_LONG_OPTION+Words.NO_STRICT+").");
                    LOGGER.severe("Exiting");
                System.exit(1);
                }
            } else {
                LOGGER.info(databases.size() + " databases were found");
            }
            for (Database database : databases) {
                databasesStore.removeDatabase(database.getDatabaseName());
                LOGGER.info("The database (" + database.getDatabaseName() + ") was removed");
            }

        }
        LOGGER.info("Bye !");


    }
}
