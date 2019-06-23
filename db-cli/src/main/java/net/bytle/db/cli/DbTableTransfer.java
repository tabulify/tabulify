package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.loader.ResultSetLoader;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.System.exit;
import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableTransfer {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;

    private static final String SOURCE_TABLE_NAME_ARG = "SourceTableName";
    private static final String TARGET_TABLE_NAME_ARG = "TargetTableName";


    public static void run(CliCommand cliCommand, String[] args) {


        String description = "Transfer a table from a database to another database.\n\n" +
                "If the target table does not exist, it will be created";
        cliCommand
                .setDescription(description);

        cliCommand.argOf(SOURCE_TABLE_NAME_ARG)
                .setDescription("The table name of the source")
                .setMandatory(true);
        cliCommand.argOf(TARGET_TABLE_NAME_ARG)
                .setDescription("The table name of the target. The default value is the name of the source table")
                .setMandatory(false);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION)
                .setMandatory(true);
        cliCommand.optionOf(JDBC_URL_SOURCE_OPTION)
                .setMandatory(true);

        cliCommand.optionOf(JDBC_DRIVER_SOURCE_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        cliCommand.optionOf(TARGET_WORKER_OPTION);
        cliCommand.optionOf(TARGET_TABLE_OPTION);
        cliCommand.optionOf(TARGET_SCHEMA_OPTION);
        cliCommand.optionOf(SOURCE_SCHEMA_OPTION);
        cliCommand.optionOf(BUFFER_SIZE_OPTION);
        cliCommand.optionOf(COMMIT_FREQUENCY_OPTION);
        cliCommand.optionOf(TARGET_BATCH_SIZE_OPTION);
        cliCommand.optionOf(TARGET_CONNECTION_SCRIPT_OPTION);
        cliCommand.optionOf(METRICS_PATH_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Target Table
        String sourceTableName = cliParser.getString(SOURCE_TABLE_NAME_ARG);
        String sourceSchemaName = cliParser.getString(SOURCE_SCHEMA_OPTION);

        String targetTableName = cliParser.getString(TARGET_TABLE_NAME_ARG);
        if (targetTableName == null) {
            targetTableName = sourceTableName;
        }
        String targetSchemaName = cliParser.getString(TARGET_SCHEMA_OPTION);


        // Metrics
        String metricsFilePath = cliParser.getString(METRICS_PATH_OPTION);

        Integer targetWorkerCount = cliParser.getInteger(TARGET_WORKER_OPTION);

        String bufferSizeString = cliParser.getString(BUFFER_SIZE_OPTION);
        Integer bufferSize = 2 * targetWorkerCount * 10000;
        if (bufferSizeString != null) {
            bufferSize = Integer.valueOf(bufferSizeString);
        } else {
            LOGGER.info(BUFFER_SIZE_OPTION + " parameter NOT found. Using default : " + bufferSize);
        }

        Integer batchSize = cliParser.getInteger(TARGET_BATCH_SIZE_OPTION);
        Integer commitFrequency = cliParser.getInteger(COMMIT_FREQUENCY_OPTION);


        // Source Connection
        String sourceUrl = cliParser.getString(JDBC_URL_SOURCE_OPTION);
        String sourceDriver = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);
        Database sourceDatabase = Databases.get(Db.CLI_DATABASE_NAME_SOURCE)
                .setUrl(sourceUrl)
                .setDriver(sourceDriver);

        TableDef sourceTableDef = sourceDatabase.getTable(sourceTableName, sourceSchemaName);
        if (!Tables.exists(sourceTableDef)) {
            sourceDatabase.close();
            String msg = "The table (" + sourceTableDef.getFullyQualifiedName() + ") doesn't exist in the database !!!!";
            LOGGER.severe(msg);
            System.err.println(msg);
            exit(1);
        }

        // Target object building
        String targetUrl = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String targetDriver = cliParser.getString(JDBC_DRIVER_TARGET_OPTION);
        Database targetDatabase = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(targetUrl)
                .setDriver(targetDriver);
        TableDef targetTableDef = targetDatabase.getTable(targetTableName, targetSchemaName);


        // Starting the load
        Date startTime = new Date();
        System.out.println("Copying the table " + sourceTableDef.getFullyQualifiedName() + " into the following table " + targetTableDef.getFullyQualifiedName());


        // Check if we have a table otherwise we will create it
        LOGGER.info("Checking if we have a target table " + targetTableDef.getFullyQualifiedName());
        if (!Tables.exists(targetTableDef)) {
            LOGGER.info("Target table does not exist, creating it");
            // Creating the table with the result set metadata
            targetTableDef = targetDatabase.getTable(targetTableDef.getName(), sourceTableDef, targetTableDef.getSchema().getName());
            Tables.create(targetTableDef);
            LOGGER.info("Target table created");
        } else {
            LOGGER.info("Target table already exists");
        }

        // The load
        List<InsertStreamListener> streamListeners = new ResultSetLoader(targetTableDef, sourceTableDef)
                .targetWorkerCount(targetWorkerCount)
                .bufferSize(bufferSize)
                .batchSize(batchSize)
                .commitFrequency(commitFrequency)
                .metricsFilePath(metricsFilePath)
                .load();


        Date endTime = new Date();
        long totalDiff = endTime.getTime() - startTime.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = 1000 * 60;
        long hoursInMilli = 1000 * 60 * 60;

        long elapsedHours = totalDiff / hoursInMilli;

        long diff = totalDiff % hoursInMilli;
        long elapsedMinutes = diff / minutesInMilli;

        diff = diff % minutesInMilli;
        long elapsedSeconds = diff / secondsInMilli;

        diff = diff % secondsInMilli;
        long elapsedMilliSeconds = diff;

        System.out.printf("Response Time for the load of the table (" + targetTableName + ") with (" + targetWorkerCount + ") target workers: %d:%d:%d.%d (hour:minutes:seconds:milli)%n", elapsedHours, elapsedMinutes, elapsedSeconds, elapsedMilliSeconds);
        System.out.printf("       Ie (%d) milliseconds%n", totalDiff);

        // Close Resources
        targetDatabase.close();
        sourceDatabase.close();

        int exitStatus = streamListeners.stream().mapToInt(s -> s.getExitStatus()).sum();
        if (exitStatus != 0) {
            System.err.println("Error ! (" + exitStatus + ") errors were seen.");
            System.out.println("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(exitStatus);
        } else {
            System.out.println("Success ! No errors were seen.");
        }


    }


}
