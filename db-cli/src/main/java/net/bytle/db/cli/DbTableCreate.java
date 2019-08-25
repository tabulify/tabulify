package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.SchemaDataUri;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.regexp.Globs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;

public class DbTableCreate {
    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    static final String TABLE_URIS = "TableUri...";
    private static final String DATA_DEF_PATH = "data-def";

    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand
                .setDescription("Create a table from data definition file(s)");

        cliCommand.argOf(TABLE_URIS)
                .setDescription("one or more Table Uri (@database[/schema]/table)")
                .setMandatory(true);


        cliCommand.optionOf(DATA_DEF_PATH)
                .setDescription("A path to a data definition file (DataDef.yml) or a parent directory")
                .setMandatory(true);

        cliCommand.optionOf(STORAGE_PATH);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Schema
        String arg = cliParser.getString(TABLE_URIS);


        Path dataDefPath = cliParser.getPath(DATA_DEF_PATH);
        if (!Files.exists(dataDefPath)) {
            LOGGER.severe("The file/directory (" + dataDefPath.toAbsolutePath().toString() + ") does not exist");
            System.exit(1);
        }
        List<TableDef> tables = DataDefs.load(dataDefPath);

        if (tables.size() == 0) {
            LOGGER.warning("The data definition file location (" + dataDefPath.toAbsolutePath().toString() + ") contains no data definition.");
        }

        CliTimer cliTimer = CliTimer.getTimer(arg).start();
        List<String> tableUris = cliParser.getStrings(TABLE_URIS);
        for (String tableUriAsString : tableUris) {

            LOGGER.info("Processing the table(s) for the table URI (" + tableUriAsString + ")");

            SchemaDataUri schemaDataUri = SchemaDataUri.of(arg);
            Database database = databasesStore.getDatabase(schemaDataUri.getDatabaseName());
            final String schemaName = schemaDataUri.getSchemaName();
            SchemaDef schemaDef = database.getCurrentSchema();
            if (schemaName != null) {
                schemaDef = database.getSchema(schemaName);
            }


            TableDataUri tableUri = TableDataUri.of(tableUriAsString);
            SchemaDef finalSchemaDef = schemaDef;
            List<TableDef> tableDefs = tables.stream()
                    .filter(t -> Globs.matches(t.getName(),tableUri.getTableName()))
                    .map(t->t.setSchema(finalSchemaDef))
                    .collect(Collectors.toList());

            if (tableDefs.size()==0){
                LOGGER.severe("No tables data definition was found for the table name pattern ("+tableUri+")");
                System.exit(1);
            } else {
                for (TableDef tableDef : tableDefs) {
                    LOGGER.info("Creating the table (" + tableDef.getFullyQualifiedName() + ")");
                    Tables.create(tableDef);
                    LOGGER.info("Table (" + tableDef.getFullyQualifiedName() + ") created.");
                }
            }

        }

        cliTimer.stop();

        LOGGER.info("Response Time for the creation of the tables : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


        LOGGER.info("Success ! No errors were seen.");

    }

}
