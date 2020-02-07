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

import static net.bytle.db.cli.Words.*;

/**
 * To download data on the local file system
 *
 * TODO: same download code than {@link DbQueryDownload} - Do we merge the code ?
 */
public class DbTableDownload {


  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableDownload.class);
  ;
  private static final String CLOB_OPTION = "cif";


  public static void run(CliCommand cliCommand, String[] args) {

    cliCommand.setDescription("Download one or more table(s) into one or more csv file(s).");
    cliCommand.argOf(SOURCE_DATA_URI)
      .setDescription("A data URI pattern that defines the tables to download")
      .setMandatory(true);
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A data URI that defines the location of the downloaded file (Example: data.csv@file or dir/@file). If the target is a directory, the name of the files will be the name of the tables.")
      .setDefaultValue(".")
      .setMandatory(true);
    cliCommand.addExample(Strings.multiline(
      "To download the table `time` from the data store `sqlite` into the file `time.csv`, you would execute",
      cliCommand.getName() + " time@sqlite time.csv"
    ));
    cliCommand.addExample(Strings.multiline(
      "To download all the table that starts with `dim` from the data store `oracle` into the directory `dim`, you would execute",
      cliCommand.getName() + " dim*@oracle dim/",
      "In non strict mode, if the directory does not exist or if a file exists, they will be replaced"
    ));
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will replace the files existing and not throw an errors for minor problem (example if a table was not found with a pattern) ")
      .setDefaultValue(false);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String sourceDataUriPatternArg = cliParser.getString(SOURCE_DATA_URI);
    final String targetDataUriArg = cliParser.getString(TARGET_DATA_URI);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      // Source
      List<DataPath> queryDataPaths = tabular.select(sourceDataUriPatternArg);
      if (queryDataPaths.size() == 0) {
        String msg = String.format("There was no tables (data paths) selected with the data uri pattern (%s)", sourceDataUriPatternArg);
        if (notStrictRunArg) {
          LOGGER.warn(msg);
          System.exit(0);
        } else {
          LOGGER.error(msg);
          System.exit(1);
        }
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
