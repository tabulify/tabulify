package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Tables;
import net.bytle.db.move.ResultSetLoader;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.MoveListener;
import net.bytle.db.stream.MemoryInsertStream;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.db.uri.TableDataUri;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.exit;
import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * transfer a table into a db
 */
public class DbTableTransfer {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String SOURCE_TABLE_URIS = "SOURCE_TABLE_URIS";
    private static final String TARGET_SCHEMA_URI = "TARGET_SCHEMA_URI";
    private static final String TARGET_TABLE_NAMES = "TargetTableNames";


    public static void run(CliCommand cliCommand, String[] args) {


        String description = "Transfer a table from a database to another database.\n\n" +
                "If the target table does not exist, it will be created";
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TARGET_SCHEMA_URI)
                .setDescription("A schema URI that defines the destination")
                .setMandatory(false);

        cliCommand.argOf(SOURCE_TABLE_URIS)
                .setDescription("One or more table URIs that define the table(s) to transfer")
                .setMandatory(true);

        cliCommand.optionOf(TARGET_TABLE_NAMES)
                .setDescription("One or more table names separated by a comma (The target table name default to the source table name)")
                .setMandatory(false);


        cliCommand.flagOf(NO_STRICT)
                .setDescription("if set, it will not throw an error if a table is not found with the source table Uri")
                .setDefaultValue(false);

        cliCommand.optionOf(DATABASE_STORE);

        // Load options
        cliCommand.optionOf(TARGET_WORKER_OPTION);
        cliCommand.optionOf(BUFFER_SIZE_OPTION);
        cliCommand.optionOf(COMMIT_FREQUENCY_OPTION);
        cliCommand.optionOf(TARGET_BATCH_SIZE_OPTION);
        cliCommand.optionOf(TARGET_CONNECTION_SCRIPT_OPTION);
        cliCommand.optionOf(METRICS_DATA_URI_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);


        // Load options
        String metricsFilePath = cliParser.getString(METRICS_DATA_URI_OPTION);
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

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Command option
        final Boolean notStrict = cliParser.getBoolean(NO_STRICT);

        // Get the tables to transfer
        List<String> tableUris = cliParser.getStrings(SOURCE_TABLE_URIS);
        List<TableDef> tablesToTransfer = new ArrayList<>();
        for (String tableUri : tableUris) {
            TableDataUri tableDataUri = TableDataUri.of(tableUri);
            Database database = databasesStore.getDatabase(tableDataUri.getDataStore());
            List<SchemaDef> schemaDefs = database.getSchemas(tableDataUri.getSchemaName());
            if (schemaDefs.size() == 0) {
                schemaDefs = Arrays.asList(database.getCurrentSchema());
            }
            for (SchemaDef schemaDef : schemaDefs) {
                List<TableDef> tablesFound = schemaDef.getTables(tableDataUri.getTableName());
                if (tablesFound.size() != 0) {

                    tablesToTransfer.addAll(tablesFound);

                } else {

                    final String msg = "No tables found with the name/pattern (" + tableUri + ")";
                    if (notStrict) {

                        LOGGER.warning(msg);

                    } else {

                        LOGGER.severe(msg);
                        exit(1);

                    }

                }
            }
        }

        // Target
        String targetTableUriOpt = cliParser.getString(TARGET_SCHEMA_URI);
        SchemaDataUri tableDataUri = SchemaDataUri.of(targetTableUriOpt);
        Database targetDatabase = databasesStore.getDatabase(tableDataUri.getDataStore());
        SchemaDef targetSchemaDef = targetDatabase.getCurrentSchema();
        if (tableDataUri.getSchemaName() != null) {
            targetSchemaDef = targetDatabase.getSchema(tableDataUri.getSchemaName());
        }


        CliTimer totalCliTimer = CliTimer.getTimer("total").start();


        LOGGER.info("Processing the request");

        TableDef executionTable = Tables.get("executions");
        executionTable
                .addColumn("Source Table Name", Types.VARCHAR)
                .addColumn("Target Table Name", Types.VARCHAR)
                .addColumn("Latency (ms)", Types.INTEGER)
                .addColumn("Row Count", Types.INTEGER)
                .addColumn("Error", Types.VARCHAR)
                .addColumn("Message", Types.VARCHAR);
        InsertStream exeInput = MemoryInsertStream.get(executionTable);

        int errorCounter = 0;
        for (TableDef tableDef : tablesToTransfer) {

            CliTimer cliTimer = CliTimer.getTimer(tableDef.getFullyQualifiedName()).start();
            Integer rowCount = null;
            String status = "";
            String message = "";
            TableDef targetTableDef = targetSchemaDef.getTableOf(tableDef.getName());
            try {

                QueryDef queryDef = targetSchemaDef.getQuery("select * from " + tableDef.getFullyQualifiedName());

                List<MoveListener> streamListeners = new ResultSetLoader(targetTableDef, queryDef)
                        .targetWorkerCount(targetWorkerCount)
                        .bufferSize(bufferSize)
                        .batchSize(batchSize)
                        .commitFrequency(commitFrequency)
                        .metricsFilePath(metricsFilePath)
                        .load();


                int exitStatus = streamListeners.stream().mapToInt(MoveListener::getExitStatus).sum();
                errorCounter += exitStatus;
                if (exitStatus != 0) {
                    status = "Err";
                }

                rowCount = streamListeners.stream().mapToInt(MoveListener::getRowCount).sum();
            } catch (Exception e) {
                errorCounter++;
                status = "Err";
                message = Log.onOneLine(e.getMessage());
                LOGGER.severe(e.getMessage());
            }

            cliTimer.stop();
            exeInput.insert(
                    tableDef.getFullyQualifiedName(),
                    targetTableDef.getFullyQualifiedName(),
                    cliTimer.getResponseTimeInMilliSeconds(),
                    rowCount,
                    status,
                    message);

        }
        exeInput.close();
        System.out.println();
        Tables.print(executionTable);
        System.out.println();

        totalCliTimer.stop();
        LOGGER.info("Response Time to query the data: " + totalCliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + totalCliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");

        if (errorCounter > 0) {
            System.err.println(errorCounter + " Errors during table transfer executions were seen");
            System.exit(1);
        }

    }


}
