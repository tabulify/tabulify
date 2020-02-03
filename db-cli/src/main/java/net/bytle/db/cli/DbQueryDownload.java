package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.engine.Queries;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.transfer.Transfer;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;
import net.bytle.timer.Timer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbQueryDownload {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String SOURCE_DATA_URI = Words.SOURCE_DATA_URI;
    private static final String DOWNLOAD_DIRECTORY = "--download-directory";
    private static final String ARG_NAME = "sqlDataUri...| (sourceDataUri targetDataUri)*";

    private static final String TARGET_SOURCE_MODE = "--source-target-mode";



    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.optionOf(SOURCE_DATA_URI)
                .setDescription("Only in default mode, A data Uri that defines the connection where all queries should run. It works with the default mode.");
        cliCommand.argOf(ARG_NAME)
                .setDescription("In default mode, a succession of sql files defined by one or more data Uri" + System.lineSeparator() +
                        "In target/source mode, a succession of query data uri followed by its target uri");
        cliCommand.optionOf(DOWNLOAD_DIRECTORY)
                .setDescription("Only in default mode, a directory that defines where the data should be downloaded");
        cliCommand.flagOf(TARGET_SOURCE_MODE)
                .setDescription("if this flag is present, the source target mode will be used (ie a succession of source/target data uri as argument)");
        String example = cliCommand.getName() + CliParser.PREFIX_LONG_OPTION + SOURCE_DATA_URI + " @sqlite QueryToDownload.sql \n";
        cliCommand.addExample(example);
        cliCommand.optionOf(DATASTORE_VAULT_PATH);

        // Parse
        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        // The data with the transfers
        List<Transfer> transfers;

        // Mode
        Boolean targetSourceMode = cliParser.getBoolean(TARGET_SOURCE_MODE);
        if (!targetSourceMode) {
            LOGGER.info("Mode: Default, download of one or several query file");

            // Container Source Data Path where the query will run
            String stringSourceDataUri = cliParser.getString(SOURCE_DATA_URI);
            if (stringSourceDataUri == null) {
                throw new RuntimeException("In the default mode, the option (" + SOURCE_DATA_URI + ") is mandatory");
            }
            DataUri sourceDataUri = DataUri.of(stringSourceDataUri);
            DataPath sourceDataPath = DataPaths.of(datastoreVault, sourceDataUri);

            // Output
            DataPath dataPathDownloadLocation;
            String downloadPathArg = cliParser.getString(OUTPUT_DATA_URI);
            if (downloadPathArg != null) {
                dataPathDownloadLocation = DataPaths.of(DataUri.of(downloadPathArg));
            } else {
                dataPathDownloadLocation = DataPaths.of(Paths.get("."));
            }

            List<String> sqlFileUris = cliParser.getStrings(ARG_NAME);
            if (sqlFileUris.size() == 0) {
                System.err.println("In default mode, at minimum a file data uri must be given");
                CliUsage.print(cliCommand);
                System.exit(1);
            }

            // Creating the transfer map (ie the source query data path/target data path)
            transfers = new ArrayList<>();
            for (String stringSqlFileUri : sqlFileUris) {

                // Source
                DataUri sqlFileUri = DataUri.of(stringSqlFileUri);
                List<DataPath> sqlDataPaths = DataPaths.select(datastoreVault, sqlFileUri);
                for (DataPath sqlDataPath : sqlDataPaths) {
                    String sourceFileQuery = Tabulars.getString(sqlDataPath);
                    if (!Queries.isQuery(sourceFileQuery)) {
                        System.err.println("The data path (" + sqlDataPaths + ") does not contains a query.");
                        CliUsage.print(cliParser.getCommand());
                        System.exit(1);
                    }
                    DataPath sourceQueryDataPath = DataPaths.ofQuery(sourceDataPath, sourceFileQuery);

                    // Target
                    DataPath targetDataPath = DataPaths.childOf(dataPathDownloadLocation, sqlDataPath.getName());

                    // Transfer
                    transfers.add(
                            Transfer.of()
                                    .setSourceDataPath(sourceQueryDataPath)
                                    .setTargetDataPath(targetDataPath)
                                    .setTransferProperties(TransferProperties.of())
                    );

                }

            }

        } else {
            LOGGER.info("Mode: Target/Source, source uri followed by the target Uri.");
            throw new RuntimeException("Not yet implemented, sorry");
        }


        System.out.println("Download process started");

        Timer totalTimer = Timer.getTimer("total").start();

        List<TransferListener> transferListeners = TransferManager.transfers(transfers);

        totalTimer.stop();

        System.out.printf("Response Time to download the data: %s (hour:minutes:seconds:milli)%n", totalTimer.getResponseTime());
        System.out.printf("       Ie (%d) milliseconds%n", totalTimer.getResponseTimeInMilliSeconds());

        // Exit
        long exitStatus = transferListeners
                .stream()
                .mapToInt(TransferListener::getExitStatus)
                .count();

        if (exitStatus != 0) {
            LOGGER.severe("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(Math.toIntExact(exitStatus));
        } else {
            LOGGER.info("Success ! No errors were seen.");
        }

    }


}
