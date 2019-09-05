package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.SchemaDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static net.bytle.db.cli.Words.DATABASE_STORE;
import static net.bytle.db.cli.Words.NO_STRICT;


public class DbSampleDrop {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String SAMPLE_SCHEMA_NAME = "SampleSchemaName";
    private static final String SCHEMA_URI = "SchemaUri";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Drop the tables that belongs to a sample database schema.";


        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(SAMPLE_SCHEMA_NAME)
                .setDescription("The name of the sample schema. One of " + String.join(", ", DbSamples.getNames()))
                .setMandatory(true);

        cliCommand.argOf(SCHEMA_URI)
                .setDescription("A relational schema uri (ie @database[/schema]")
                .setMandatory(true);

        cliCommand.optionOf(DATABASE_STORE);

        cliCommand.flagOf(Words.NO_STRICT)
                .setDescription("if set, it will not throw an error if a table is not found")
                .setDefaultValue(false);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        SchemaDataUri schemaUri = SchemaDataUri.of(cliParser.getString(SCHEMA_URI));
        Database database = databasesStore.getDatabase(schemaUri.getDatabaseName());
        SchemaDef schemaDef = database.getCurrentSchema();
        if (schemaUri.getSchemaName()!=null){
            schemaDef = database.getSchema(schemaUri.getSchemaName());
        }

        String sampleName = cliParser.getString(SAMPLE_SCHEMA_NAME);
        if (!DbSamples.getNames().contains(sampleName)) {
            System.err.println();
            System.err.println("The sample schema (" + sampleName + ") is unknown");
            System.err.println("It must be one of (" + String.join(", ", DbSamples.getNames()) + ")");
            System.exit(1);
        }

        Dag dag = Dag.get(DbSamples.getTables(sampleName));
        List<TableDef> tables = dag.getDropOrderedTables();

        Boolean noSrictMode = cliParser.getBoolean(NO_STRICT);
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        for (TableDef tableDef : tables) {
            if (Tables.exists(tableDef, schemaDef)) {
                Tables.drop(tableDef, schemaDef);
                System.out.println("Table (" + tableDef.getName() + ") was dropped.");
            } else {
                final String msg = "Table (" + tableDef.getName() + ") doesn't exist.";
                if(noSrictMode) {
                    System.out.println(msg);
                } else {
                    System.err.println(msg);
                    System.out.println("You can retry this run with the no-strict mode flag ("+NO_STRICT+") to avoid an error if a table does not exist.");
                    System.exit(1);
                }
            }
        }

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        LOGGER.info("Bye !");


    }


}
