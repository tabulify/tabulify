package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.gen.DataGenLoader;
import net.bytle.db.gen.yml.DataGenYml;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;



/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableFill {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    static final String NUMBER_OF_ROWS_OPTION = "rows";
    private static String DEFINITION_FILE = "data-def";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Load generated data into a table\n\n"+
                "By default, the data would be randomly generated.\n"+
                "You should use a data definition file (option "+DEFINITION_FILE+") to have more control over the data generated";


        final String ARG_NAME = "(DatabaseUri|TableUri)";
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("A table Uri (@database[/schema]/table) or a database/schema URI (@database[/schema])")
                .setMandatory(true);

        cliCommand.optionOf(STORAGE_PATH);

        cliCommand.optionOf(DEFINITION_FILE)
                .setDescription("A path to a data definition file (DataDef.yml)");

        cliCommand.optionOf(NUMBER_OF_ROWS_OPTION)
                .setDescription("defines the total number of rows that the table must have")
                .setDefaultValue(100);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Arg
        String arg = cliParser.getString(ARG_NAME);
        Path dataDefPath = cliParser.getPath(DEFINITION_FILE);


        if (dataDefPath == null) {
            LOGGER.info("Loading generated data for the table " + arg);
        } else {
            LOGGER.info("Loading generated data with the data definition file (" + dataDefPath + ")");
        }

        TableDataUri tableDataUri = TableDataUri.of(arg);
        Database database = databasesStore.getDatabase(tableDataUri.getDatabaseName());
        final String schemaName = tableDataUri.getSchemaName();
        SchemaDef schemaDef = database.getCurrentSchema();
        if (schemaName!=null) {
            schemaDef = database.getSchema(schemaName);
        }
        CliTimer cliTimer = CliTimer.getTimer(arg).start();


        if (dataDefPath!=null) {
            if (!(Files.exists(dataDefPath))){
                LOGGER.severe("The file ("+dataDefPath.toAbsolutePath().toString()+" does not exist");
                System.exit(1);
            }
            InputStream input;
            try {
                input = Files.newInputStream(dataDefPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            DataGenYml dataGenYml = new DataGenYml(schemaDef, input).loadParentTable(true);
            List<TableDef> tables = dataGenYml.load();

            LOGGER.info("The following tables where loaded:");
            for (TableDef tableDef : tables) {
                LOGGER.info("  * " + tableDef.getFullyQualifiedName() + ", Size (" + Tables.getSize(tableDef) + ")");
            }

        } else {

            TableDef tableDef = schemaDef.getTableOf(tableDataUri.getTableName());
            if (!Tables.exists(tableDef)) {
                LOGGER.severe("The table (" + tableDef.getFullyQualifiedName() + " doesn't exist.");
                System.exit(1);
            } else {
                LOGGER.info("The table (" + tableDef.getFullyQualifiedName() + ") has (" + Tables.getSize(tableDef) + ") rows before loading.");
            }

            Integer totalNumberOfRows = cliParser.getInteger(NUMBER_OF_ROWS_OPTION);
            DataGenLoader.get(tableDef)
                    .setRows(totalNumberOfRows)
                    .load();

            //LOGGER.info("The table (" + tableDef.getFullyQualifiedName() + ") has now (" + Tables.getSize(tableDef) + ") rows");
        }

        cliTimer.stop();

        LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


        LOGGER.info("Success ! No errors were seen.");

    }


}
