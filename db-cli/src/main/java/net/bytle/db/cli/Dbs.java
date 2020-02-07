package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.transfer.TransferOptions;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dbs {

  private static final Logger LOGGER = LoggerFactory.getLogger(Dbs.class);

  public static Map<DataStore, List<DataPath>> collectDataPathsByDataStore(Tabular tabular, List<String> dataUriPatterns, Boolean notStrictRun, CliCommand cliCommand){
    Map<DataStore, List<DataPath>> dataPathsByDataStores = new HashMap<>();
    for (String dataUriPattern : dataUriPatterns) {
      List<DataPath> dataPathsByPattern = tabular.select(dataUriPattern);

      if (dataPathsByPattern.size() == 0) {
        String msg = "The data uri pattern (" + dataUriPattern + ") is not a pattern that select tables";
        if (notStrictRun) {
          LOGGER.warn(msg);
        } else {
          LOGGER.error(msg);
          CliUsage.print(cliCommand);
          System.exit(1);
        }
      } else {

        DataStore dataStore = dataPathsByPattern.get(0).getDataSystem().getDataStore();

        List<DataPath> dataPathsByDataStore = dataPathsByDataStores.computeIfAbsent(dataStore, k -> new ArrayList<>());
        dataPathsByDataStore.addAll(dataPathsByPattern);

      }
    }
    return dataPathsByDataStores;
  }

  public static List<DataPath> collectDataPaths(Tabular tabular, List<String> dataUriPatterns, Boolean notStrictRun, CliCommand cliCommand){
    List<DataPath> dataPathsCollected = new ArrayList<>();
    for (String dataUriPattern : dataUriPatterns) {
      List<DataPath> dataPathsByPattern = tabular.select(dataUriPattern);
      if (dataPathsByPattern.size() == 0) {
        String msg = "The data uri pattern (" + dataUriPattern + ") is not a pattern that select tables";
        if (notStrictRun) {
          LOGGER.warn(msg);
        } else {
          LOGGER.error(msg);
          CliUsage.print(cliCommand);
          System.exit(1);
        }
      } else {
        dataPathsCollected.addAll(dataPathsByPattern);
      }
    }
    return dataPathsCollected;
  }

  public static void transfers(String sourceDataUriPatternArg, String targetDataUriArg, Path storagePathValue, Boolean notStrictRunArg) {

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
