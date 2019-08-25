package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DataUri;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.SchemaDataUri;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.gen.DataDefLoader;
import net.bytle.db.gen.DataGenDef;
import net.bytle.db.gen.DataGenLoader;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableFill {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    static final String NUMBER_OF_ROWS_OPTION = "rows";
    static String DEFINITION_FILE = "data-def";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Load generated data into a table\n\n" +
                "By default, the data would be randomly generated.\n" +
                "You should use a data definition file (option " + DEFINITION_FILE + ") to have more control over the data generated";


        final String TABLE_OR_SCHEMA_URI = "(SchemaUri|TableUri)";
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TABLE_OR_SCHEMA_URI)
                .setDescription("A schema URI (@database[/schema]) when loading with a data definition file or a table Uri (@database[/schema]/table) without.")
                .setMandatory(true);

        cliCommand.optionOf(STORAGE_PATH);

        cliCommand.optionOf(DEFINITION_FILE)
                .setDescription("A path to a data definition file (DataDef.yml)");

        cliCommand.optionOf(NUMBER_OF_ROWS_OPTION)
                .setDescription("defines the total number of rows that the table(s) must have")
                .setMandatory(false);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Arg
        String dataUri = cliParser.getString(TABLE_OR_SCHEMA_URI);
        Path dataDefPath = cliParser.getPath(DEFINITION_FILE);


        if (dataDefPath == null) {
            LOGGER.info("Loading generated data for the table " + dataUri);
        } else {
            LOGGER.info("Loading generated data with the data definition file (" + dataDefPath + ")");
        }

        Integer totalNumberOfRows = cliParser.getInteger(NUMBER_OF_ROWS_OPTION);

        CliTimer cliTimer = CliTimer.getTimer("Fill table").start();


        if (dataDefPath != null) {

            // The data uri must be a schema URI, the name of the table is in the data def
            SchemaDataUri schemaDataUri = SchemaDataUri.of(dataUri);

            Database database = databasesStore.getDatabase(schemaDataUri.getDatabaseName());
            final String schemaName = schemaDataUri.getSchemaName();
            SchemaDef schemaDef = database.getCurrentSchema();
            if (schemaName != null) {
                schemaDef = database.getSchema(schemaName);
            }

            if (!(Files.exists(dataDefPath))) {
                LOGGER.severe("The file (" + dataDefPath.toAbsolutePath().toString() + " does not exist");
                System.exit(1);
            }

            SchemaDef finalSchemaDef = schemaDef;
            List<DataGenDef> dataGenDefs = DataDefs.load(dataDefPath)
                    .stream()
                    .map(t->t.setSchema(finalSchemaDef))
                    .map(t -> DataGenDef.get(t))
                    .map(t->totalNumberOfRows!=null?t.setRows(totalNumberOfRows):t)
                    .collect(Collectors.toList());

            List<DataGenDef> loadedDataGenDefs = DataDefLoader.of(schemaDef)
                    .loadParentTable(true)
                    .load(dataGenDefs);

            LOGGER.info("The following tables where loaded:");
            for (DataGenDef dataGenDef : loadedDataGenDefs) {
                LOGGER.info("  * " + dataGenDef.getTableDef().getFullyQualifiedName() + ", Size (" + Tables.getSize(dataGenDef.getTableDef()) + ")");
            }

        } else {

            // No data def, it must be a data uri
            TableDataUri tableDataUri = TableDataUri.of(dataUri);

            Database database = databasesStore.getDatabase(tableDataUri.getDatabaseName());
            final String schemaName = tableDataUri.getSchemaName();
            SchemaDef schemaDef = database.getCurrentSchema();
            if (schemaName != null) {
                schemaDef = database.getSchema(schemaName);
            }


            TableDef tableDef = schemaDef.getTableOf(tableDataUri.getTableName());
            if (!Tables.exists(tableDef)) {
                LOGGER.severe("The table (" + tableDef.getFullyQualifiedName() + " doesn't exist.");
                System.exit(1);
            } else {
                LOGGER.info("The table (" + tableDef.getFullyQualifiedName() + ") has (" + Tables.getSize(tableDef) + ") rows before loading.");
            }



            DataGenDef datagenDef = DataGenDef.get(tableDef).setRows(totalNumberOfRows);
            DataGenLoader.get(datagenDef).load();

        }

        cliTimer.stop();

        LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


        LOGGER.info("Success ! No errors were seen.");

    }


}
