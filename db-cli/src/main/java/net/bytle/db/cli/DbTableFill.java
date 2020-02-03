package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.Database;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.gen.DataGeneration;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.log.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableFill {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    static final String NUMBER_OF_ROWS_OPTION = "rows";
    static final String LOAD_PARENT = "load-parent";



    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Load generated data into one or more tables\n\n" +
                "By default, the data would be randomly generated.\n" +
                "You should use the data definition file option (" + Words.DEFINITION_FILE + ") to define the data generation behaviors.";


        final String TABLE_URIS = "TableUri...";
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more table URI (@database[/schema]/table) from the same schema.")
                .setMandatory(true);

        cliCommand.optionOf(DATASTORE_VAULT_PATH);
        cliCommand.optionOf(Words.FORCE)
            .setDescription("The FORCE mode will not emit an error if a table is not found for a Table URI");

        cliCommand.flagOf(LOAD_PARENT)
                .setDescription("If this flag is present, the foreign table(s) will not be loaded if not present in the table Uri selection.")
                .setDefaultValue(true);


        cliCommand.optionOf(Words.DEFINITION_FILE)
                .setDescription("A path to a data definition file (DataDef.yml) or a directory containing several data definition file.");

        cliCommand.optionOf(NUMBER_OF_ROWS_OPTION)
                .setDescription("defines the total number of rows that the table(s) must have")
                .setMandatory(false);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Load parent
        Boolean loadParent = cliParser.getBoolean(LOAD_PARENT);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);


        // Data Definition
        Path dataDefPath = cliParser.getPath(Words.DEFINITION_FILE);
        if (dataDefPath == null) {
            LOGGER.info("Loading generated data without data definition file");
        } else {
            LOGGER.info("Loading generated data with the data definition file option (" + dataDefPath + ")");
            if (!(Files.exists(dataDefPath))) {
                LOGGER.severe("The file (" + dataDefPath.toAbsolutePath().toString() + " does not exist");
                System.exit(1);
            }
        }

        // Force
        Boolean force = cliParser.getBoolean(Words.FORCE);

        // Number of rows
        Integer totalNumberOfRows = cliParser.getInteger(NUMBER_OF_ROWS_OPTION);

        LOGGER.info("Starting filling the tables");
        CliTimer cliTimer = CliTimer.getTimer("Fill table").start();


        // Data Uri (same schema) to table to load
        // The tables to load
        Set<TableDef> tablesToLoad = new TreeSet<>();
        // The data URI
        List<String> dataUris = cliParser.getStrings(TABLE_URIS);
        Database database = null;
        SchemaDef schemaDef = null;
        for (String tableDataUriString : dataUris) {

            TableDataUri tableDataUri = TableDataUri.of(tableDataUriString);
            final Database databaseinUri = datastoreVault.getDataStore(tableDataUri.getDataStore());
            if (database == null) {
                database = databaseinUri;
            } else {

                if (!database.equals(databaseinUri)) {
                    LOGGER.severe("The table uri's should define the same database");
                    LOGGER.severe("We have found that the database ("+database+") and the database ("+databaseinUri+") are not the same.");
                    System.exit(1);
                }
            }
            final String schemaName = tableDataUri.getSchemaName();
            if (schemaName != null) {
                final SchemaDef schemaInUri = database.getSchema(schemaName);
                if (schemaDef==null) {
                    schemaDef = schemaInUri;
                } else {
                    if (!schemaDef.equals(schemaInUri)){
                        LOGGER.severe("The table uri's should define in the same schema");
                        LOGGER.severe("We have found that the schema ("+schemaDef.getFullyQualifiedName()+") and the schema ("+schemaInUri.getFullyQualifiedName()+") are not the same.");
                        System.exit(1);
                    }
                }
            } else {
                schemaDef = database.getCurrentSchema();
            }

            LOGGER.info("Searching the table for the table Uri (" + tableDataUriString + ")");
            List<TableDef> tableDefs = schemaDef.getTables(tableDataUri.getTableName());
            if (tableDefs.size() == 0) {

                final String msg = "No table(s) were found with the table Uri (" + tableDataUri + ")";
                if (!force) {
                    LOGGER.severe(msg);
                    System.exit(1);
                } else {
                    LOGGER.warning(msg);
                }

            }

            // Get the tables to load for a certain database
            for (TableDef tableDef : tableDefs) {
                tablesToLoad.add(tableDef);
                LOGGER.info("The table (" + tableDef.getFullyQualifiedName() + ") has been added to the table to be loaded.");
            }

        }

        // Merge the properties
        if (dataDefPath!=null) {
            // Create a map of the dataDef use in the next merge step
            Map<String, TableDef> dataDefTableDefs = DataDefs.of().load(dataDefPath)
                    .stream()
                    .collect(Collectors.toMap(TableDef::getName, Function.identity()));
            // Merge
            tablesToLoad = tablesToLoad.stream()
                    .map(s -> dataDefTableDefs.containsKey(s.getName()) ? Tables.mergeProperties(s, dataDefTableDefs.get(s.getName())) : s)
                    .collect(Collectors.toSet());
        }

        List<TableDef> tablesLoaded = DataGeneration.of()
                .addTables(new ArrayList<>(tablesToLoad), totalNumberOfRows)
                .loadParentTable(loadParent)
                .load();

        LOGGER.info("The following tables where loaded:");
        for (TableDef tableDef : tablesLoaded) {
            LOGGER.info("  * " + tableDef.getFullyQualifiedName() + ", Size (" + Tables.getSize(tableDef) + ")");
        }

        cliTimer.stop();

        LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


        LOGGER.info("Success ! No errors were seen.");

    }


}
