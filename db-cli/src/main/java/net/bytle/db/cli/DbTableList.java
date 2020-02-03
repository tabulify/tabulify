package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.log.Log;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.Database;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;

import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbTableList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URIS = "TableUri...";


        public static void run(CliCommand cliCommand, String[] args) {

        cliCommand
                .setDescription("Print a list of tables.");

        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more name table uri (ie @database[/schema]/table)")
                .setMandatory(true);

        cliCommand.optionOf(DATASTORE_VAULT_PATH);

        cliCommand.flagOf(Words.NO_COUNT)
                .setDescription("suppress the column showing the table count")
                .setShortName("c");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        List<String> stringTableUris = cliParser.getStrings(TABLE_URIS);
        List<TableDef> tableDefs = new ArrayList<>();

        for (String stringTableUri : stringTableUris) {
            TableDataUri tableDataUri = TableDataUri.of(stringTableUri);
            Database database = datastoreVault.getDataStore(tableDataUri.getDataStore());
            SchemaDef schemaDef = database.getCurrentSchema();
            if (tableDataUri.getSchemaName()!=null) {
                schemaDef = database.getSchema(tableDataUri.getSchemaName());
            }
            tableDefs.addAll(schemaDef.getTables(tableDataUri.getTableName()));

        }
        if (tableDefs.size() == 0) {
            LOGGER.info("No tables found");
        }

        // Construct the table result
        // The table result structure
        final Boolean noCountColumn = cliParser.getBoolean(Words.NO_COUNT);
        TableDef printTable = Tables.get("tables")
                .addColumn("Table Name");
        if (!noCountColumn) {
            printTable.addColumn("Rows Count", Types.INTEGER);
        }

        InsertStream insertStream = Tables.getTableInsertStream(printTable);
        for (TableDef tableDef : tableDefs) {

            if (!noCountColumn) {
                Integer count = Tables.getSize(tableDef);
                insertStream.insert(tableDef.getName(), count);
            } else {
                insertStream.insert(tableDef.getName());
            }
        }
        insertStream.close();

        System.out.println();
        Tables.print(printTable);
        Tables.drop(printTable);
        System.out.println();

        LOGGER.info("Bye !");


}


}
