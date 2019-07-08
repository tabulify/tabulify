package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Relations;
import net.bytle.db.loader.ResultSetLoader;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableLoad {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {


        String description = "Load a file into a database.";

        String footer = "The last argument is a CSV file";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);


        cliCommand.getGroup("Connection")
                .setLevel(2)
                .addWordOf(DB_NAME);

        cliCommand.getGroup("Target table")
                .setLevel(2)
                .addWordOf(TARGET_TABLE_OPTION);

        cliCommand.getGroup("Load option")
                .setLevel(2)
                .addWordOf(TARGET_WORKER_OPTION)
                .addWordOf(BUFFER_SIZE_OPTION)
                .addWordOf(COMMIT_FREQUENCY_OPTION)
                .addWordOf(TARGET_BATCH_SIZE_OPTION);

        cliCommand.getGroup("Monitoring option")
                .setLevel(2)
                .addWordOf(METRICS_PATH_OPTION);

        String argName = "File.csv";

        cliCommand.argOf(argName)
                .setDescription("A CSV file")
                .setMandatory(true);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Source
        Path inputFilePath = null;
        String fileSourcePathArg = cliParser.getString(argName);
        int postPoint = fileSourcePathArg.lastIndexOf(".");
        if (postPoint == -1) {
            LOGGER.severe("The file must have an extension.");
            System.exit(1);
        } else {

            String fileExtension = fileSourcePathArg.substring(postPoint + 1, fileSourcePathArg.length()).toLowerCase();

            if (fileExtension.equals("csv")) {
                inputFilePath = Paths.get(fileSourcePathArg);
            } else {
                LOGGER.severe("Actually only CSV files are supported (" + fileSourcePathArg + ").");
                CliUsage.print(cliCommand);
                System.exit(1);
            }
        }


        // Target Connection
        String targetUrl = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String targetDriver = cliParser.getString(JDBC_DRIVER_TARGET_OPTION);
        Database targetDatabase = Databases.of(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(targetUrl)
                .setDriver(targetDriver);

        // Target Table
        String targetTableName = cliParser.getString(Words.TARGET_TABLE_OPTION);
        if (targetTableName == null) {
            Path fileName = inputFilePath.getFileName();
            targetTableName = fileName.toString().substring(0, fileName.toString().lastIndexOf("."));
            LOGGER.info("The option (" + Words.TARGET_TABLE_OPTION + " was not defined. The table name (" + targetTableName + ") was taken from the input file (" + fileName + ").");
        }
        TableDef targetTable = targetDatabase.getTable(targetTableName);

        // Metrics
        String metricsFilePath = cliParser.getString(Words.METRICS_PATH_OPTION);

        String targetWorkerCountString = cliParser.getString(Words.TARGET_WORKER_OPTION);
        Integer targetWorkerCount = 1;
        if (targetWorkerCountString != null) {
            targetWorkerCount = Integer.valueOf(targetWorkerCountString);
        } else {
            LOGGER.info(Words.TARGET_WORKER_OPTION + " parameter NOT found. Using the default : " + targetWorkerCount);
        }

        String bufferSizeString = cliParser.getString(Words.BUFFER_SIZE_OPTION);
        Integer bufferSize = 2 * targetWorkerCount * 10000;
        if (bufferSizeString != null) {
            bufferSize = Integer.valueOf(bufferSizeString);
        } else {
            LOGGER.info(BUFFER_SIZE_OPTION + " parameter NOT found. Using default : " + bufferSize);
        }

        String batchSizeString = cliParser.getString(TARGET_BATCH_SIZE_OPTION);
        Integer batchSize = 10000;
        if (batchSizeString != null) {
            batchSize = Integer.valueOf(batchSizeString);
        } else {
            LOGGER.info(TARGET_BATCH_SIZE_OPTION + " parameter NOT found. Using default : " + batchSize);
        }


        String commitFrequencyString = cliParser.getString(COMMIT_FREQUENCY_OPTION);
        Integer commitFrequency = 99999;
        if (commitFrequencyString != null) {
            commitFrequency = Integer.valueOf(commitFrequencyString);
        } else {
            LOGGER.info(COMMIT_FREQUENCY_OPTION + " parameter NOT found. Using default : " + commitFrequency);
        }


        CliTimer cliTimer = CliTimer.getTimer(targetTableName)
                .start();

        LOGGER.info("Loading Table " + targetTableName);

        RelationDef relationDef = Relations.get(inputFilePath);

        List<InsertStreamListener> resultSetListeners = new ResultSetLoader(targetTable, relationDef)
                .targetWorkerCount(targetWorkerCount)
                .bufferSize(bufferSize)
                .batchSize(batchSize)
                .commitFrequency(commitFrequency)
                .metricsFilePath(metricsFilePath)
                .load();

        cliTimer.stop();

        LOGGER.info("Response Time for the load of the table (" + targetTableName + ") with (" + targetWorkerCount + ") target workers: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");

        int exitStatus = resultSetListeners.stream().mapToInt(s -> s.getExitStatus()).sum();
        if (exitStatus != 0) {
            LOGGER.severe("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(exitStatus);
        } else {
            LOGGER.info("Success ! No errors were seen.");
        }


    }


}
