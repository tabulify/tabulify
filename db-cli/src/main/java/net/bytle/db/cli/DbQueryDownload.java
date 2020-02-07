package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.transfer.TransferOptions;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;

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
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will replace the files existing and not throw an errors for minor problem (example if a table was not found with a pattern) ")
      .setDefaultValue(false);

    // Parse and args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);
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

      // Source
      List<DataPath> queryDataPaths = tabular.select(sourceQueryUriArg);
      if (queryDataPaths.size() == 0) {
        LOGGER.error("There was no sql file selected with the query uri pattern ({})", sourceQueryUriArg);
        System.exit(1);
      }
      // Target
      DataPath targetDataPath = tabular.getDataPath(targetDataUriArg);

      // Transfer Properties
      TransferProperties transferProperties = TransferProperties.of();
      if (notStrictRunArg) {
        transferProperties
          .addTargetOperations(TransferOptions.CREATE_IF_NOT_EXIST)
          .addTargetOperations(TransferOptions.DROP_IF_EXIST);
      }

      // Transfer
      LOGGER.info("Starting the download process");
      Timer totalTimer = Timer.getTimer("total").start();
      List<TransferListener> transferListeners = TransferManager.of()
        .addTransfers(queryDataPaths, targetDataPath)
        .setTransferProperties(transferProperties)
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
