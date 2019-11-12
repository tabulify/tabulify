package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.log.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Dag;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.transfer.MoveListener;
import net.bytle.db.stream.MemorySelectStream;
import net.bytle.db.stream.Streams;
import net.bytle.db.tpc.TpcdsDgen;

import java.nio.file.Path;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATABASE_STORE;


public class DbSampleFill {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String SAMPLE_SCHEMA_NAME = "SampleSchemaName";
    private static final String SCHEMA_URI = "SchemaUri";
    static final String SCALE = "scale";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "(Create|Load) data into a sample database schema.";


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

        cliCommand.optionOf(SCALE)
                .setShortName("s")
                .setDescription("The size of the generated data in Gb (works only for tpc schema)")
                .setDefaultValue(0.01);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        SchemaDataUri schemaUri = SchemaDataUri.of(cliParser.getString(SCHEMA_URI));
        Database database = databasesStore.getDatabase(schemaUri.getDataStore());
        SchemaDef schemaDef = database.getCurrentSchema();
        if (schemaUri.getSchemaName()!=null){
            schemaDef = database.getSchema(schemaUri.getSchemaName());
        }

        // Placed here to show the args at the beginning of the output
        Double scale = cliParser.getDouble(SCALE);

        String sampleName = cliParser.getString(SAMPLE_SCHEMA_NAME);
        if (!DbSamples.getNames().contains(sampleName)) {
            System.err.println("The sample schema (" + sampleName + ") is unknown");
            System.err.println("It must be one of (" + String.join(", ", DbSamples.getNames()) + ")");
            System.exit(1);
        }

        Dag dag = Dag.get(DbSamples.getTables(sampleName));
        List<TableDef> tableDefs = dag.getCreateOrderedTables();


        LOGGER.info("Checking if all tables exist.");
        for (TableDef tableDef : tableDefs) {
            if (!Tables.exists(tableDef, database)) {
                System.err.println("The table (" + tableDef.getName() + ") does not exist in the target database (" + database.getDatabaseName() + ")");
                System.err.println("Possible cause:");
                System.err.println("  * The schema tables have not been created with the 'create' command.");
                System.exit(1);
            } else {
                System.out.println("The table (" + tableDef.getName() + ") exists in the target database (" + database.getDatabaseName() + ")");
            }
        }
        // load
        List<MoveListener> insertStreamListeners = TpcdsDgen.get()
                .setSchema(schemaDef)
                .setScale(scale)
                .setFeedbackFrequency(5)
                .load(tableDefs);

        // Sort by table name
        insertStreamListeners = insertStreamListeners
                .stream()
                .sorted((s1, s2) -> (s1.getInsertStream().getRelationDef().getName().compareTo(s2.getInsertStream().getRelationDef().getName())))
                .collect(Collectors.toList());

        // Feedback
        TableDef printTable = Tables.get("result")
                .addColumn("Tables Loaded")
                .addColumn("Rows Inserted", Types.INTEGER);

        InsertStream printTableInsertStream = Tables.getTableInsertStream(printTable);
        for (MoveListener insertStreamListener : insertStreamListeners) {
            printTableInsertStream.insert(
                    insertStreamListener.getInsertStream().getRelationDef().getName()
                    , insertStreamListener.getRowCount()
            );
        }
        printTableInsertStream.close();

        System.out.println("Results:");
        MemorySelectStream outputStream = Tables.getTableOutputStream(printTable);
        Streams.print(outputStream);
        outputStream.close();

        // Error ?
        Integer errors = insertStreamListeners
                .stream()
                .mapToInt(s -> s.getExitStatus()>0?1:0)
                .sum();

        if (errors>0){
            LOGGER.severe(errors+" errors were seen during data loading");
            System.exit(1);
        } else {
            LOGGER.info("No errors were seen");
        }
        LOGGER.info("Bye !");


    }


}
