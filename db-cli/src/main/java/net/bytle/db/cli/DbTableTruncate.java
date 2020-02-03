package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.log.Log;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.Database;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


public class DbTableTruncate {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URIS = "TableUri...";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Truncate table(s) - ie remove all records from a table";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TABLE_URIS)
                .setDescription("one or more table URI (@database[/schema]/table).");
        cliCommand.optionOf(DATASTORE_VAULT_PATH);
        cliCommand.flagOf(Words.FORCE)
                .setDescription("truncate also the tables that references the truncated tables")
                .setDefaultValue(false);
        cliCommand.flagOf(Words.NO_STRICT)
                .setDescription("if set, it will not throw an error if a table is not found")
                .setDefaultValue(false);

        CliParser cliParser = Clis.getParser(cliCommand, args);
        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        Boolean noStrictMode = cliParser.getBoolean(Words.NO_STRICT);
        final List<String> stringTablesUris = cliParser.getStrings(TABLE_URIS);
        List<TableDef> tablesSelectedToTruncate = new ArrayList<>();
        for (String stringTableUri : stringTablesUris) {
            TableDataUri tableUri = TableDataUri.of(stringTableUri);
            Database database = datastoreVault.getDataStore(tableUri.getDataStore());
            SchemaDef schemaDef = database.getCurrentSchema();
            if (tableUri.getSchemaName() != null) {
                schemaDef = database.getSchema(tableUri.getSchemaName());
            }
            final List<TableDef> tables = schemaDef.getTables(tableUri.getTableName());
            if (tables.size() == 0) {
                String msg = "No tables was found for the pattern (" + tableUri.getTableName() + ")";
                if (noStrictMode) {
                    LOGGER.warning(msg);
                } else {
                    LOGGER.severe(msg);
                    LOGGER.severe("If you don't want to exit when a table is not found, you can use the no-strict flag (" + Words.NO_STRICT + ")");
                    LOGGER.severe("Exiting");
                    System.exit(1);
                }
            }
            tablesSelectedToTruncate.addAll(tables);
        }

        Boolean forceMode = cliParser.getBoolean(Words.FORCE);
        // Do we have also the child/external table ?
        // sqlite is not enforcing the foreign keys, we need then to do it in the code
        List<TableDef> tablesToTruncate = new ArrayList<>(tablesSelectedToTruncate);
        for (
                TableDef tableDef : tablesSelectedToTruncate) {
            List<TableDef> childTables = tableDef.getExternalForeignKeys()
                    .stream()
                    .map(d -> d.getTableDef())
                    .collect(Collectors.toList());
            for (TableDef childTable : childTables) {
                if (!(tablesSelectedToTruncate.contains(childTable))) {
                    final String msg = "The table (" + childTable + ") has a foreign key into the table to truncate (" + tableDef + ") but is not selected";
                    if (!forceMode) {
                        LOGGER.severe(msg);
                        LOGGER.severe("If you want to truncate also the tables that reference the selected tables, you can set the force flag (" + Words.FORCE + ")");
                        LOGGER.severe("exiting");
                        System.exit(1);
                    } else {
                        LOGGER.warning(msg);
                        LOGGER.warning("We are running in force mode, the table (" + childTable + ") was added to the tables to truncate");
                        tablesToTruncate.add(childTable);
                    }
                }
            }
        }

        // Truncating
        for (TableDef tableDef : ForeignKeyDag.get(tablesToTruncate).getDropOrderedTables()) {
            Tables.truncate(tableDef);
        }
        LOGGER.info("Bye !");

    }


}

