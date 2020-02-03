package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.database.Database;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


/**
 * <p>
 */
public class DbDatastoreList {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbDatastoreList.class);

    private static final String DATASTORE_PATTERN = "name|glob";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List the databases present in the store";

        // Create the parser
        cliCommand
                .setDescription(description);


        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(DATASTORE_PATTERN)
                .setDescription("a sequence of database name or a glob pattern")
                .setMandatory(false)
                .setDefaultValue("*");

        cliCommand.optionOf(Words.DATASTORE_VAULT_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        final List<String> names = cliParser.getStrings(DATASTORE_PATTERN);

        final List<DataStore> datastore = datastoreVault.getDataStores(names);

        // Creating a table to use the print function
        TableDef databaseInfo = Tabular.tabular().getDataPath("databases")
                .getDataDef()
                .addColumn("Name")
                .addColumn("Login")
                .addColumn("Password")
                .addColumn("Url")
                .addColumn("Driver");

        try (InsertStream tableInsertStream = Tabulars.getInsertStream(databaseInfo.getDataPath())) {
            for (DataStore dataStore : datastore) {
                String password = null;
                if (dataStore.getPassword() != null) {
                    password = "xxx";
                }
                tableInsertStream
                        .insert(dataStore.getName(), dataStore.getUser(), password, dataStore.getConnectionString(), ((Database) dataStore).getDriver());
            }
        }

        System.out.println();
        Tabulars.print(databaseInfo.getDataPath());
        System.out.println();

        Tabulars.delete(databaseInfo.getDataPath());


        LOGGER.info("Bye !");


    }


}
