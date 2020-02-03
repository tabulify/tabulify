package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataStore;
import net.bytle.db.database.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.INFO_COMMAND;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbDatastoreInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbDatastoreInfo.class);
    private static final String DATASTORE_NAMES = "@databaseUri...";
    private static final String HORIZONTAL_LINE = "-------------------------------";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Print datastore information in a form fashion.";

        String footer = "Example:\n\n"+
                "\tTo output information about the datastore `name`:\n" +
                "\t\tdb " + Words.DATASTORE_COMMAND + " " + INFO_COMMAND + " @name\n\n"+
                "\tTo output information about all the datastore with `sql` in their name:\n"+
                "\t\tdb " + Words.DATASTORE_COMMAND + " " + INFO_COMMAND + " sql*\n";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);

        cliCommand.argOf(DATASTORE_NAMES)
                .setDescription("one or more database name")
                .setDefaultValue("*");

        cliCommand.optionOf(Words.DATASTORE_VAULT_PATH);

        cliCommand.optionOf(Words.FORCE)
                .setDescription("If there is no database found, no errors will be emitted");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        // Retrieve
        List<String> dataStoreNames = cliParser.getStrings(DATASTORE_NAMES);
        final List<DataStore> dataStores = datastoreVault.getDataStores(dataStoreNames);

        Boolean force = cliParser.getBoolean(Words.FORCE);

        if (dataStores.size()==0){
            if (force){
                LOGGER.warn("No datastore was found with the name ("+dataStoreNames+")");
                return;
            } else {
                LOGGER.error("No datastore was found with the name (" + dataStoreNames + ")");
                System.exit(1);
            }
        }
        //Print
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        System.out.println();
        System.out.println(HORIZONTAL_LINE);
        for (int i=0; i<dataStores.size();i++) {
            DataStore dataStore = dataStores.get(i);
            System.out.println("Name: "+dataStore.getName());
            System.out.println("URL: "+dataStore.getConnectionString());
            System.out.println("Login: "+dataStore.getUser());
            String password;
            if (dataStore.getPassword()!=null){
                password="xxx";
            } else {
                password="null";
            }
            System.out.println("Password: "+password);
            if (dataStore.getScheme().equals(Database.JDBC_SCHEME)) {
              System.out.println("Driver: " + ((Database) dataStore).getDriver());
              System.out.println("Statement: " + ((Database) dataStore).getPostConnectionStatement());
            }
            System.out.println(HORIZONTAL_LINE);
            // TODO The below would print jdbc driver information
            // database.printDatabaseInformation();
        }
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        System.out.println();
        LOGGER.info("Bye !");


    }


}
