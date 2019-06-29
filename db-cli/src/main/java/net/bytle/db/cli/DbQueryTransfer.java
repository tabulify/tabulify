package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.loader.ResultSetLoader;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * main class to load the result of a Query into a table
 */
public class DbQueryTransfer {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand command, String[] args) {


        String footer = "\nExample:\n " + CliUsage.getFullChainOfCommand(command) + " " +
                "-su \"jdbc:oracle:thin:scott/tiger@host:1521:sid\" " +
                "-tu \"jdbc:sap://host:30015/?user=login&password=pwd\" " +
                "-tt TableName";


        command
                .setDescription("transfer the result of query")
                .setFooter(footer);
        final String ARG = "query.sql";
        command.argOf(ARG)
                .setDescription("A file containing a query")
                .setMandatory(true);

        command.optionOf(JDBC_URL_SOURCE_OPTION);
        command.optionOf(SOURCE_FETCH_SIZE_OPTION);
        command.optionOf(JDBC_DRIVER_SOURCE_OPTION);
        command.optionOf(SOURCE_QUERY_OPTION);
        command.optionOf(JDBC_URL_TARGET_OPTION);
        command.optionOf(JDBC_DRIVER_TARGET_OPTION);
        command.optionOf(TARGET_TABLE_OPTION)
                .setDescription("Define the table name (Default to the name of the file");
        command.optionOf(TARGET_WORKER_OPTION);
        command.optionOf(BUFFER_SIZE_OPTION);
        command.optionOf(COMMIT_FREQUENCY_OPTION);
        command.optionOf(TARGET_BATCH_SIZE_OPTION);
        command.optionOf(METRICS_PATH_OPTION);


        // create the parser
        CliParser cliParser = Clis.getParser(command, args);


        // Target Table
        String targetTableName = cliParser.getString(TARGET_TABLE_OPTION);
        if (targetTableName == null) {
            String fileName = cliParser.getPath(ARG).getFileName().toString();
            targetTableName = fileName.substring(0, fileName.lastIndexOf("."));
        }

        // Source Data
        // Source Database
        String sourceUrl = cliParser.getString(JDBC_URL_SOURCE_OPTION);
        String sourceDriver = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);
        Database sourceDatabase = Databases.get(Db.CLI_DATABASE_NAME_SOURCE)
                .setUrl(sourceUrl)
                .setDriver(sourceDriver);
        // Source Query
        String query = cliParser.getFileContent(ARG);
        // Query
        QueryDef sourceQueryDef = sourceDatabase.getQuery(query);


        // Target Connection
        String targetUrl = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String targetDriver = cliParser.getString(JDBC_DRIVER_TARGET_OPTION);

        Database targetDatabase = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(targetUrl)
                .setDriver(targetDriver);


        // Metrics
        String metricsFilePath = cliParser.getString(METRICS_PATH_OPTION);

        Integer targetWorkerCount = cliParser.getInteger(TARGET_WORKER_OPTION);


        Integer bufferSize = cliParser.getInteger(BUFFER_SIZE_OPTION);
        if (bufferSize == null) {
            bufferSize = 2 * targetWorkerCount * 10000;
            LOGGER.info(BUFFER_SIZE_OPTION + " parameter NOT found. Using default : " + bufferSize);

        }
        Integer batchSize = cliParser.getInteger(TARGET_BATCH_SIZE_OPTION);
        Integer fetchSize = cliParser.getInteger(SOURCE_FETCH_SIZE_OPTION);
        Integer commitFrequency = cliParser.getInteger(COMMIT_FREQUENCY_OPTION);
        TableDef targetTableDef = targetDatabase.getTable(targetTableName);

        CliTimer cliTimer = CliTimer.getTimer(targetTableName).start();

        LOGGER.info("Loading Table " + targetTableName);

        List<InsertStreamListener> resultSetListener;

        resultSetListener = new ResultSetLoader(targetTableDef, sourceQueryDef)
                .addTableAttribute("table_type", "COLUMN") // TODO: COLUMN is for SAP HANA - move this to the creation of the table
                .targetWorkerCount(targetWorkerCount)
                .bufferSize(bufferSize)
                .batchSize(batchSize)
                .commitFrequency(commitFrequency)
                .metricsFilePath(metricsFilePath)
                .load();


        cliTimer.stop();

        LOGGER.info("Response Time for the load of the table (" + targetTableName + ") with (" + targetWorkerCount + ") target workers: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");

        int exitStatus = resultSetListener.stream().mapToInt(s -> s.getExitStatus()).sum();
        if (exitStatus != 0) {
            LOGGER.severe("Error ! (" + exitStatus + ") errors were seen.");
        } else {
            LOGGER.info("Success ! No errors were seen.");
        }

    }
}
