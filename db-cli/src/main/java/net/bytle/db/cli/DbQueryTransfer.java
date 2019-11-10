package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.move.ResultSetLoader;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.uri.DataUri;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.log.Log;

import java.nio.file.Files;
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

        CliOptions.addCopyOptions(command);



        command.argOf(Words.SOURCE_DATA_URI)
                .setDescription("A Data Uri (@dataStore) where the queries will be executed")
                .setMandatory(true);

        command.argOf(TARGET_DATA_URI)
                .setDescription("A Data Uri (name@dataStore) (by default, the target name will be the name of the query file)")
                .setMandatory(true);

        command.argOf(FILE_URI)
                .setDescription("A Data URI defining sql file(s) or a directory containing queries (Example: query.sql or dim*.sql)")
                .setMandatory(true);

        // Create the parser
        CliParser cliParser = Clis.getParser(command, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Target Table
        DataPath targetDataPath = DataPaths.of(databasesStore, DataUri.of(cliParser.getString(TARGET_DATA_URI)));

        DataPath sourceDataPath = DataPaths.of(databasesStore, DataUri.of(cliParser.getString(SOURCE_DATA_URI)));

        List<DataPath> fileDataPaths = 
        String query = cliParser.getFileContent(FILE_URI);
        DataPath queryDataPath = DataPaths.ofQuery(sourceDataPath,query);




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
