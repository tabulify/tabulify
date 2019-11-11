package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Relations;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.move.Move;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.move.MoveListener;
import net.bytle.db.uri.IDataUri;
import net.bytle.log.Log;

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
    private static final String FILE_URI = "FILE_URIS";
    private static final String SCHEMA_URI = "SCHEMA_URI";
    private static final String TARGET_TABLE_NAMES = "TargetTableNames";

    public static void run(CliCommand cliCommand, String[] args) {


        // Create the parser
        cliCommand
                .setDescription("Load one ore more files into a database.");

        cliCommand.optionOf(DATABASE_STORE);

        cliCommand.getGroup("Load option")
                .setLevel(2)
                .addWordOf(TARGET_WORKER_OPTION)
                .addWordOf(BUFFER_SIZE_OPTION)
                .addWordOf(COMMIT_FREQUENCY_OPTION)
                .addWordOf(TARGET_BATCH_SIZE_OPTION);

        cliCommand.getGroup("Monitoring option")
                .setLevel(2)
                .addWordOf(METRICS_DATA_URI_OPTION);


        cliCommand.flagOf(NO_STRICT)
                .setDescription("if set, it will not throw an error if a table is not found with the source table Uri")
                .setDefaultValue(false);

        cliCommand.optionOf(TARGET_TABLE_NAMES)
                .setDescription("One or more table names separated by a comma (The target table name default to the source table name)")
                .setMandatory(false);

        cliCommand.argOf(SCHEMA_URI)
                .setDescription("A schema Uri that define (Example: @databaseName[/schema]).")
                .setMandatory(true);

        cliCommand.argOf(FILE_URI)
                .setDescription("A file URI that define one or more CSV file(s)")
                .setMandatory(true);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Source
        Path inputFilePath = null;
        String fileSourcePathArg = cliParser.getString(FILE_URI);
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
        final IDataUri IDataUri = SchemaDataUri.of(cliParser.getString(SCHEMA_URI));
        final TableDataUri tableDataUri;
        if (IDataUri.getPathSegments().length > 0){
            tableDataUri = TableDataUri.of(IDataUri);
        } else {
            Path fileName = inputFilePath.getFileName();
            String targetTableName = fileName.toString().substring(0, fileName.toString().lastIndexOf("."));
            LOGGER.info("The table name was not defined. The table name (" + targetTableName + ") was taken from the input file (" + fileName + ").");
            tableDataUri = TableDataUri.of(IDataUri.get(IDataUri.toString(),targetTableName));
        }
        Database targetDatabase = databasesStore.getDatabase(tableDataUri.getDataStore());

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



        CliTimer cliTimer = CliTimer.getTimer(tableDataUri.getTableName())
                .start();

        LOGGER.info("Loading Table " + tableDataUri.getTableName());

        RelationDef relationDef = Relations.get(inputFilePath);

        List<MoveListener> resultSetListeners = new Move(targetTable, relationDef)
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
