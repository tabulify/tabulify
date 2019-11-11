package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.move.MoveProperties;
import net.bytle.db.move.ResultSetLoader;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.uri.DataUri;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        CliOptions.addMoveOptions(command);


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


        // Query
        DataPath queryDataPath = DataPaths.of(databasesStore, DataUri.of(cliParser.getString(FILE_URI)));
        List<DataPath> queryDataPaths = new ArrayList<>();
        if (Tabulars.isContainer(queryDataPath)){
            queryDataPaths.addAll(DataPaths.getChildren(queryDataPath,"*.sql"));
        } else {
            queryDataPaths.add(queryDataPath);
        }

        // Source Data Path
        DataPath sourceDataPath = DataPaths.of(databasesStore, DataUri.of(cliParser.getString(SOURCE_DATA_URI)));
        Map<String, DataPath> sourceDataPaths = new HashMap<>();
        if (Tabulars.isDocument(sourceDataPath)){
            throw new RuntimeException("The source data Uri ("+SOURCE_DATA_URI+" should represent a schema/catalog not a table");
        } else {
            sourceDataPaths = queryDataPaths.stream()
                    .collect(Collectors.toMap(
                            d->d.getName(),
                            d->DataPaths.ofQuery(sourceDataPath,Tabulars.getString(d))
                            ));
        }

        // Target Table
        DataPath targetDataPath = DataPaths.of(databasesStore, DataUri.of(cliParser.getString(TARGET_DATA_URI)));
        List<DataPath> targetDataPaths = new ArrayList<>();
        if (Tabulars.isContainer(targetDataPath)){
            targetDataPaths = queryDataPaths.stream()
                    .map(d->DataPaths.childOf(targetDataPath,d.getName()))
                    .collect(Collectors.toList());
        } else {
            if (queryDataPaths.size()!=1){
                LOGGER.warning("All ("+queryDataPaths.size()+") queries will be loaded in one table ("+targetDataPath.toString()+")");
                targetDataPaths = IntStream.of(queryDataPaths.size())
                        .mapToObj(s->targetDataPath)
                        .collect(Collectors.toList());
            } else {
                targetDataPaths = queryDataPaths
                        .stream()
                        .map(s->DataPaths.childOf(targetDataPath,s.getName()))
                        .collect(Collectors.toList());
            }
        }

        MoveProperties moveProperties = CliOptions.getMoveOptions(cliParser);
        int i = 0;
        for (Map.Entry<String,DataPath> entry: sourceDataPaths.entrySet()){

            String queryName = entry.getKey();
            CliTimer cliTimer = CliTimer.getTimer("query "+queryName).start();
            DataPath source = entry.getValue();
            DataPath target = targetDataPaths.get(0);
            LOGGER.info("Loading the query (" + queryName +") from the source ("+sourceDataPath+") into the table ("+target.toString());


            List<InsertStreamListener> resultSetListener;

            resultSetListener = new ResultSetLoader(targetTableDef, sourceQueryDef)
                    .targetWorkerCount(targetWorkerCount)
                    .bufferSize(bufferSize)
                    .batchSize(batchSize)
                    .commitFrequency(commitFrequency)
                    .metricsFilePath(metricsFilePath)
                    .load();

            LOGGER.info("Response Time for the load of the table (" + targetTableName + ") with (" + targetWorkerCount + ") target workers: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
            LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");

            cliTimer.stop();

        }












        int exitStatus = resultSetListener.stream().mapToInt(s -> s.getExitStatus()).sum();
        if (exitStatus != 0) {
            LOGGER.severe("Error ! (" + exitStatus + ") errors were seen.");
        } else {
            LOGGER.info("Success ! No errors were seen.");
        }

    }
}
