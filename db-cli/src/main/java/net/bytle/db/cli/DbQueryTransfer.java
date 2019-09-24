package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.loader.ResultSetLoader;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.uri.SchemaDataUri;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * main class to load the result of a Query into a table
 */
public class DbQueryTransfer {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String FILE_URI = "query.sql";

    public static void run(CliCommand command, String[] args) {


        String footer = "\nExample:\n " + CliUsage.getFullChainOfCommand(command) + " " +
                "@sqlite " +
                "@sqlserver " +
                "query.sql";


        command
                .setDescription("Transfer the result of query from one source database to a target database")
                .setFooter(footer);

        command.optionOf(DATABASE_STORE);
        command.optionOf(SOURCE_FETCH_SIZE_OPTION);
        command.optionOf(SOURCE_QUERY_OPTION);
        command.optionOf(BUFFER_SIZE_OPTION);
        command.optionOf(TARGET_WORKER_OPTION);
        command.optionOf(COMMIT_FREQUENCY_OPTION);
        command.optionOf(TARGET_BATCH_SIZE_OPTION);
        command.optionOf(METRICS_PATH_OPTION);


        command.argOf(Words.SOURCE_DATA_URI)
                .setDescription("A schema Uri (@database[/schema]) where the queries will be executed")
                .setMandatory(true);

        command.argOf(TARGET_DATA_URI)
                .setDescription("A table Uri (@database[/schema]/tableName) or a schema Uri (@database[/schema]) (in this case, the target table name will be the name of the query file)")
                .setMandatory(true);

        command.argOf(FILE_URI)
                .setDescription("A file URI defining sql file(s) or a directory containing queries (Example: query.sql or dim*.sql)")
                .setMandatory(true);

        // create the parser
        CliParser cliParser = Clis.getParser(command, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Target Table
        String targetTableName = cliParser.getString(TARGET_DATA_URI);
        if (targetTableName == null) {
            String fileName = cliParser.getPath(FILE_URI).getFileName().toString();
            targetTableName = fileName.substring(0, fileName.lastIndexOf("."));
        }

        SchemaDataUri sourceSchemaDataUri = SchemaDataUri.of(SOURCE_DATA_URI);
        Database sourceDatabase = databasesStore.getDatabase(sourceSchemaDataUri.getDatabaseName());
        SchemaDef sourceSchemaDef = sourceDatabase.getCurrentSchema();
        if (sourceSchemaDataUri.getSchemaName()!=null){
            sourceSchemaDef = sourceDatabase.getSchema(sourceSchemaDataUri.getSchemaName());
        }

        // Source Query
        String query = cliParser.getFileContent(FILE_URI);
        // Query
        QueryDef sourceQueryDef = sourceSchemaDef.getQuery(query);


        Database targetDatabase = Databases.of(Db.CLI_DATABASE_NAME_TARGET);


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
