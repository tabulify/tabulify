package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;


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
        cliCommand.optionOf(STORAGE_PATH);
        cliCommand.flagOf(Words.FORCE)
                .setDescription("truncate also the tables that references the truncated tables")
                .setDefaultValue(false);

        CliParser cliParser = Clis.getParser(cliCommand, args);
        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        final List<String> stringTablesUris = cliParser.getStrings(TABLE_URIS);
        List<TableDef> tablesSelectedToTruncate = new ArrayList<>();
        for (String stringTableUri : stringTablesUris) {
            TableDataUri tableUri = TableDataUri.of(stringTableUri);
            Database database = databasesStore.getDatabase(tableUri.getDatabaseName());
            SchemaDef schemaDef = database.getCurrentSchema();
            if (tableUri.getSchemaName() != null) {
                schemaDef = database.getSchema(tableUri.getSchemaName());
            }
            tablesSelectedToTruncate.addAll(schemaDef.getTables(tableUri.getTableName()));
        }

        Boolean forceMode = cliParser.getBoolean(Words.FORCE);
        // Do we have also the child/external table ?
        List<TableDef> tablesToTruncate = new ArrayList<>(tablesSelectedToTruncate);
        for (TableDef tableDef: tablesSelectedToTruncate){
            List<TableDef> childTables = tableDef.getExternalForeignKeys()
                    .stream()
                    .map(d->d.getTableDef())
                    .collect(Collectors.toList());
            for (TableDef childTable: childTables){
                if (!(tablesSelectedToTruncate.contains(childTable))){
                    final String msg = "The table (" + childTable + ") has a foreign key into the table to truncate (" + tableDef + ") but is not selected";
                    if (!forceMode) {
                        LOGGER.severe(msg);
                        LOGGER.severe("exiting");
                        System.exit(1);
                    } else {
                        tablesToTruncate.add(childTable);
                    }
                }
            }
        }

        // Truncating
        for (TableDef tableDef: Dag.get(tablesToTruncate).getDropOrderedTables()){
            Tables.truncate(tableDef);
        }
        LOGGER.info("Bye !");

    }


}

