package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.DataStore;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.DbDatastore.BYTLE_DB_DATABASES_STORE;
import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


/**
 * <p>
 */
public class DbDatastoreRemove {

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

        cliCommand.optionOf(Words.DATASTORE_VAULT_PATH)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatastore.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_STORE);

        cliCommand.flagOf(Words.NO_STRICT)
                .setDescription("If the removed database does not exist, the command will not exit with a failure code.")
                .setDefaultValue(false);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);


        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        final Boolean noStrictMode = cliParser.getBoolean(Words.NO_STRICT);
        final List<String> names = cliParser.getStrings(DATABASE_PATTERN);
        for (String name : names) {
            final List<DataStore> dataStores = datastoreVault.getDataStores(name);
            if (dataStores.size() == 0) {
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
                LOGGER.info(dataStores.size() + " databases were found");
            }
            for (DataStore dataStore : dataStores) {
                datastoreVault.removeDataStore(dataStore.getName());
                LOGGER.info("The database (" + dataStore.getName() + ") was removed");
            }

        }
        LOGGER.info("Bye !");


    }
}
