package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;
import net.bytle.db.sample.SchemaSample;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.MemorySelectStream;
import net.bytle.db.stream.Streams;
import net.bytle.db.tpc.TpcdsDgen;

import java.sql.Types;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


public class DbSampleFill {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final String ARG_NAME = "schemaName";
    public static final String SCALE = "scale";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "(Create|Load) data into a sample database schema.";


        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("The name of the sample schema. One of " + String.join(", ", DbSamples.getNames()))
                .setMandatory(true);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);
        cliCommand.optionOf(SCALE)
                .setShortName("s")
                .setDescription("The size of the generated data in Gb (works only for tpc schema)")
                .setDefaultValue(0.01);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));

        // Placed here to show the args at the begining of the output
        Double scale = cliParser.getDouble(SCALE);

        String sampleName = cliParser.getString(ARG_NAME);
        if (!DbSamples.getNames().contains(sampleName)) {
            System.err.println("The sample schema (" + sampleName + ") is unknown");
            System.err.println("It must be one of (" + String.join(", ", DbSamples.getNames()) + ")");
            System.exit(1);
        }

        SchemaSample schemaSample = DbSamples.get(sampleName);
        Dag dag = Dag.get(schemaSample.getTables());
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
        List<InsertStreamListener> insertStreamListeners = TpcdsDgen.get()
                .setDatabase(database)
                .setScale(scale)
                .setFeedbackFrequency(5)
                .load(tableDefs);

        // Sort by table name
        insertStreamListeners = insertStreamListeners
                .stream()
                .sorted((s1, s2) -> (s1.getInsertStream().getTableDef().getName().compareTo(s2.getInsertStream().getTableDef().getName())))
                .collect(Collectors.toList());

        // Feedback
        TableDef printTable = Tables.get("result")
                .addColumn("Tables Loaded")
                .addColumn("Rows Inserted", Types.INTEGER);

        InsertStream insertStream = Tables.getTableInsertStream(printTable);
        for (InsertStreamListener insertStreamListener : insertStreamListeners) {
            insertStream.insert(
                    insertStreamListener.getInsertStream().getTableDef().getName()
                    , insertStreamListener.getRowCount()
            );
        }
        insertStream.close();

        System.out.println("Results:");
        MemorySelectStream outputStream = Tables.getTableOutputStream(printTable);
        Streams.print(outputStream);
        outputStream.close();


        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        LOGGER.info("Bye !");


    }


}
