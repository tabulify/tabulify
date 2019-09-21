package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.uri.DataUri;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Relations;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.loader.ResultSetLoader;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.uri.IDataUri;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static net.bytle.db.cli.Words.DATABASE_STORE;
import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableLoad {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String FILE = "sourceFile";
    private static final String TABLE_DATA_URI = "targetTableDataUri";


    public static void run(CliCommand cliCommand, String[] args) {


        // Create the parser
        cliCommand
                .setDescription("Load a local file into a database.");

        cliCommand.optionOf(DATABASE_STORE);

        cliCommand.getGroup("Load option")
                .setLevel(2)
                .addWordOf(TARGET_WORKER_OPTION)
                .addWordOf(BUFFER_SIZE_OPTION)
                .addWordOf(COMMIT_FREQUENCY_OPTION)
                .addWordOf(TARGET_BATCH_SIZE_OPTION);

        cliCommand.getGroup("Monitoring option")
                .setLevel(2)
                .addWordOf(METRICS_PATH_OPTION);


        cliCommand.argOf(FILE)
                .setDescription("A local path to a CSV file")
                .setMandatory(true);

        cliCommand.argOf(TABLE_DATA_URI)
                .setDescription("A table data Uri (Example: @databaseName[/schema/table]). The database name is the only mandatory property. The default schema is the default schema of the database. The default table name is the name of the file.")
                .setMandatory(true);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Source
        Path inputFilePath = null;
        String fileSourcePathArg = cliParser.getString(FILE);
        int postPoint = fileSourcePathArg.lastIndexOf(".");
        if (postPoint == -1) {
            LOGGER.severe("The file must have an extension.");
            System.exit(1);
        } else {

            String fileExtension = fileSourcePathArg.substring(postPoint + 1).toLowerCase();

            if (fileExtension.equals("csv")) {
                inputFilePath = Paths.get(fileSourcePathArg);
            } else {
                LOGGER.severe("Actually only CSV files are supported (" + fileSourcePathArg + ").");
                CliUsage.print(cliCommand);
                System.exit(1);
            }
        }

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Is there a path
        final IDataUri IDataUri = DataUri.of(cliParser.getString(TABLE_DATA_URI));
        final TableDataUri tableDataUri;
        if (IDataUri.getPathSegments().length > 0){
            tableDataUri = TableDataUri.of(IDataUri);
        } else {
            Path fileName = inputFilePath.getFileName();
            String targetTableName = fileName.toString().substring(0, fileName.toString().lastIndexOf("."));
            LOGGER.info("The table name was not defined. The table name (" + targetTableName + ") was taken from the input file (" + fileName + ").");
            tableDataUri = TableDataUri.of(IDataUri.get(IDataUri.toString(),targetTableName));
        }
        Database targetDatabase = databasesStore.getDatabase(tableDataUri.getDatabaseName());

        // Schema
        SchemaDef targetSchemaDef = targetDatabase.getCurrentSchema();
        if (tableDataUri.getSchemaName()!=null){
            LOGGER.info("The schema name ("+tableDataUri.getSchemaName()+").");
            targetSchemaDef = targetDatabase.getSchema(tableDataUri.getSchemaName());
        } else {
            LOGGER.info("The schema name was not defined. The database default schema was taken ("+targetDatabase.getCurrentSchema()+")");
        }

        // Target Table
        TableDef targetTable = targetSchemaDef.getTableOf(tableDataUri.getTableName());

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

        CliTimer cliTimer = CliTimer.getTimer(tableDataUri.getTableName())
                .start();

        LOGGER.info("Loading Table " + tableDataUri.getTableName());

        RelationDef relationDef = Relations.get(inputFilePath);

        List<InsertStreamListener> resultSetListeners = new ResultSetLoader(targetTable, relationDef)
                .targetWorkerCount(targetWorkerCount)
                .bufferSize(bufferSize)
                .batchSize(batchSize)
                .commitFrequency(commitFrequency)
                .metricsFilePath(metricsFilePath)
                .load();

        cliTimer.stop();

        LOGGER.info("Response Time for the load of the table (" + tableDataUri.getTableName() + ") with (" + targetWorkerCount + ") target workers: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");

        int exitStatus = resultSetListeners.stream().mapToInt(s -> s.getExitStatus()).sum();
        if (exitStatus != 0) {
            LOGGER.severe("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(exitStatus);
        } else {
            LOGGER.info("Success ! No errors were seen.");
        }


    }


}
