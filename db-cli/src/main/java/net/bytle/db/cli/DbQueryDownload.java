package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.engine.Queries;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbQueryDownload {


  private static final Logger LOGGER = LoggerFactory.getLogger(DbQueryDownload.class);

  private static final String SOURCE_QUERY_URI_PATTERN = "SourceQueryUriPattern";
  private static final String TARGET_DATA_URI = "targetUri";


  public static void run(CliCommand cliCommand, String[] args) {

    // Cli Command
    cliCommand.addExample(Strings.multiline(
      "To download the data of the query defined in the file `QueryToDownload.sql` and executed against the data store `sqlite` into the file `QueryData.csv`, you would execute the following command:",
      cliCommand.getName() + " QueryToDownload.sql@sqlite QueryData.csv"
    ));
    cliCommand.addExample(Strings.multiline(
      "To download the data of all query defined in all `sql` files of the current directory, execute them against the data store `sqlite` and save the results into the directory `result`, you would execute the following command:",
      cliCommand.getName() + " *.sql@sqlite result"
    ));
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.argOf(SOURCE_QUERY_URI_PATTERN)
      .setDescription("The source query URI pattern");
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A data URI that defines the destination (a file or a directory)");


    // Parse and args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String sourceQueryUriArg = cliParser.getString(SOURCE_QUERY_URI_PATTERN);
    final String targetDataUriArg = cliParser.getString(TARGET_DATA_URI);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      // Source Files selected
      DataUri firstQueryUri = DataUri.of(sourceQueryUriArg);
      List<Path> firstQueryUriFiles = Fs.getFilesByGlob(firstQueryUri.getPath());
      if (firstQueryUriFiles.size() == 0) {
        LOGGER.error("There was no sql file selected with the first query uri pattern ({})", sourceQueryUriArg);
      }
      DataStore sourceDataStore = tabular.getDataStore(firstQueryUri.getDataStore());
      List<DataPath> queryDataPaths = firstQueryUriFiles.stream()
        .map(p -> {
          String sourceFileQuery = Queries.getQuery(p);
          if (sourceFileQuery == null) {
            LOGGER.error("The path (" + p + ") does not contains a query.");
            CliUsage.print(cliParser.getCommand());
            System.exit(1);
          }
          return sourceDataStore.getQueryDataPath(sourceFileQuery);
        })
        .collect(Collectors.toList());

      // Target
      DataPath targetDataPath = tabular.getDataPath(targetDataUriArg);
      if (Tabulars.isDocument(targetDataPath) && firstQueryUriFiles.size() > 1) {
        LOGGER.error("The Query URI pattern ({}) has selected more than one query files ({}), the target data uri should be a container (directory) but is a document (file) {} ", sourceQueryUriArg, firstQueryUriFiles, targetDataPath);
        System.exit(1);
      }

      LOGGER.info("Starting download process");
      Timer totalTimer = Timer.getTimer("total").start();
      List<TransferListener> transferListeners = TransferManager.of()
        .addTransfers(queryDataPaths, targetDataPath)
        .start();
      totalTimer.stop();

      System.out.printf("Response Time to download the data: %s (hour:minutes:seconds:milli)%n", totalTimer.getResponseTime());
      System.out.printf("       Ie (%d) milliseconds%n", totalTimer.getResponseTimeInMilliSeconds());

      // Exit
      long exitStatus = transferListeners
        .stream()
        .mapToInt(TransferListener::getExitStatus)
        .count();

      if (exitStatus != 0) {
        LOGGER.error("Error ! (" + exitStatus + ") errors were seen.");
        System.exit(Math.toIntExact(exitStatus));
      } else {
        LOGGER.info("Success ! No errors were seen.");
      }
    }
  }


}
