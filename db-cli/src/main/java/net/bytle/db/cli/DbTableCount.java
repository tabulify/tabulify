package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.log.Log;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.Database;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.model.SchemaDef;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


public class DbTableCount {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URIS = "TableUri...";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Count the number of tables";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TABLE_URIS)
                .setDescription("one or more table URI (@database[/schema]/table).");
        cliCommand.optionOf(DATASTORE_VAULT_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);
        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        final List<String> stringTablesUris = cliParser.getStrings(TABLE_URIS);
        Integer count = 0;
        for (String stringTableUri : stringTablesUris) {
            TableDataUri tableUri = TableDataUri.of(stringTableUri);
            Database database = datastoreVault.getDataStore(tableUri.getDataStore());
            SchemaDef schemaDef = database.getCurrentSchema();
            if (tableUri.getSchemaName() != null) {
                schemaDef = database.getSchema(tableUri.getSchemaName());
            }
            count += schemaDef.getTables(tableUri.getTableName()).size();
        }
        System.out.println(count + " tables");

        LOGGER.info("Bye !");

    }


}

