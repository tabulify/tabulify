package net.bytle.db.cli;

import net.bytle.cli.*;
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
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;

public class DbTableCreate {
    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    static final String SCHEMA_URI = "(DatabaseUri|SchemaUri)";
    private static final String DATA_DEF_PATH = "DataDef.yml";

    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand
                .setDescription("Create a table from a data definition file");

        cliCommand.argOf(SCHEMA_URI)
                .setDescription("A Schema Uri (@database[/schema])")
                .setMandatory(true);

        cliCommand.argOf(DATA_DEF_PATH)
                .setDescription("One or more data definition file")
                .setMandatory(true);

        cliCommand.optionOf(STORAGE_PATH);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Schema
        String arg = cliParser.getString(SCHEMA_URI);
        SchemaDataUri schemaDataUri = SchemaDataUri.of(arg);
        Database database = databasesStore.getDatabase(schemaDataUri.getDatabaseName());
        final String schemaName = schemaDataUri.getSchemaName();
        SchemaDef schemaDef = database.getCurrentSchema();
        if (schemaName != null) {
            schemaDef = database.getSchema(schemaName);
        }
        LOGGER.info("The table(s) will be created in the schema (" + schemaDef.getFullyQualifiedName() + ")");

        List<String> dataDefPaths = cliParser.getStrings(DATA_DEF_PATH);
        CliTimer cliTimer = CliTimer.getTimer(arg).start();
        LOGGER.info(dataDefPaths.size() + " data definition file was specified.");
        for (String dataDefPathAsString : dataDefPaths) {


            LOGGER.info("Starting the creation of tables from the data definition file (" + dataDefPathAsString + ")");

            Path dataDefPath = Paths.get(dataDefPathAsString);
            if (!Files.exists(dataDefPath)) {
                LOGGER.severe("The file (" + dataDefPath.toAbsolutePath().toString() + ") does not exist");
                System.exit(1);
            }


            List<TableDef> tables = DataDefs.load(dataDefPath)
                    .stream()
                    .map(t -> t.setDatabase(database))
                    .collect(Collectors.toList());

            if (tables.size() == 0) {
                LOGGER.warning("The data definition file (" + dataDefPath.toAbsolutePath().toString() + ") contains no data definition.");
            }

            for (TableDef tableDef : tables) {
                LOGGER.info("Creating the table (" + tableDef.getFullyQualifiedName() + ")");
                Tables.create(tableDef);
                LOGGER.info("Table ("+tableDef.getFullyQualifiedName()+") created.");
            }


        }

        cliTimer.stop();

        LOGGER.info("Response Time for the creation of the tables : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


        LOGGER.info("Success ! No errors were seen.");

    }

}
