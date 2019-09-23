package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Tables;
import net.bytle.db.loader.ResultSetLoader;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.db.uri.TableDataUri;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    private static final String SOURCE_TABLE_URIS = "Source TableUri...";
    private static final String TARGET_SCHEMA_URI = "Target TableUri|SchemaUri";


    public static void run(CliCommand cliCommand, String[] args) {


        String description = "Transfer a table from a database to another database.\n\n" +
                "If the target table does not exist, it will be created";
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TARGET_SCHEMA_URI)
                .setDescription("A schema URI that defines the destination")
                .setMandatory(false);

        cliCommand.argOf(SOURCE_TABLE_URIS)
                .setDescription("One or more table URIs that define the table to transfer")
                .setMandatory(true);


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
        cliCommand.optionOf(METRICS_PATH_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Target Table
        String sourceTableName = cliParser.getString(SOURCE_TABLE_URIS);


        String targetTableName = cliParser.getString(TARGET_SCHEMA_URI);
        if (targetTableName == null) {
            targetTableName = sourceTableName;
        }


        // Load options
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

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Command option
        final Boolean notStrict = cliParser.getBoolean(NO_STRICT);

        // Get the tables to transfer
        List<String> tableUris = cliParser.getStrings(SOURCE_TABLE_URIS);
        List<TableDef> tablesToTransfer = new ArrayList<>();
        for (String tableUri : tableUris) {
            TableDataUri tableDataUri = TableDataUri.ofUri(tableUri);
            Database database = databasesStore.getDatabase(tableDataUri.getDatabaseName());
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
        SchemaDataUri tableDataUri = SchemaDataUri.ofUri(targetTableUriOpt);
        Database targetDatabase = databasesStore.getDatabase(tableDataUri.getDatabaseName());
        SchemaDef schemaDef = targetDatabase.getCurrentSchema();
        if (tableDataUri.getSchemaName() != null) {
            schemaDef = targetDatabase.getSchema(tableDataUri.getSchemaName());
        }


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


        int exitStatus = streamListeners.stream().mapToInt(InsertStreamListener::getExitStatus).sum();
        if (exitStatus != 0) {
            System.err.println("Error ! (" + exitStatus + ") errors were seen.");
            System.out.println("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(exitStatus);
        } else {
            System.out.println("Success ! No errors were seen.");
        }


    }


}
