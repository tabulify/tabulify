package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.log.Log;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.bytle.db.cli.Words.*;


/**
 * <p>
 * main class to load the result of a Query into a table
 */
public class DbQueryTransfer {


  private static final Log LOGGER = Db.LOGGER_DB_CLI;

  public static void run(CliCommand command, String[] args) {

    // Command
    command
      .setDescription("Transfer the result of query from one source database to a target database")
      .addExample(Strings.multiline("Transfer the result of the query `top10product` from sqlite to the table `top10product` of sql server",
        CliUsage.getFullChainOfCommand(command) + " top10product.sql@sqlite @sqlserver "
      ));
    command.optionOf(DATASTORE_VAULT_PATH);
    command.argOf(Words.SOURCE_DATA_URI)
      .setDescription("A query Uri pattern (filePattern.sql@dataStore) that defines the queries to transfer")
      .setMandatory(true);
    command.argOf(TARGET_DATA_URI)
      .setDescription("A target data Uri ([name]@dataStore) (by default, the target name will be the name of the query file)")
      .setMandatory(true);
    CliOptions.addTransferOptions(command);

    // Create the parser and get the args
    CliParser cliParser = Clis.getParser(command, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String sourceUriArg = cliParser.getString(SOURCE_DATA_URI);
    final String targetUriArg = cliParser.getString(TARGET_DATA_URI);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      // Query

      List<DataPath> queryDataPaths = new ArrayList<>();
      if (Tabulars.isContainer(queryDataPath)) {
        queryDataPaths.addAll(DataPaths.getChildren(queryDataPath, "*.sql"));
      } else {
        queryDataPaths.add(queryDataPath);
      }

      // Source Data Path
      DataPath sourceDataPath = tabular.getDataPath(cliParser.getString(SOURCE_DATA_URI));
      Map<String, DataPath> sourceDataPaths = new HashMap<>();
      if (Tabulars.isDocument(sourceDataPath)) {
        throw new RuntimeException("The source data Uri (" + SOURCE_DATA_URI + " should represent a schema/catalog not a table");
      } else {
        sourceDataPaths = queryDataPaths.stream()
          .collect(Collectors.toMap(
            d -> d.getName(),
            d -> DataPaths.ofQuery(sourceDataPath, Tabulars.getString(d))
          ));
      }

      // Target Table
      DataPath targetDataPath = tabular.getDataPath(cliParser.getString(TARGET_DATA_URI));
      List<DataPath> targetDataPaths = new ArrayList<>();
      if (Tabulars.isContainer(targetDataPath)) {
        targetDataPaths = queryDataPaths.stream()
          .map(d -> DataPaths.childOf(targetDataPath, d.getName()))
          .collect(Collectors.toList());
      } else {
        if (queryDataPaths.size() != 1) {
          LOGGER.warning("All (" + queryDataPaths.size() + ") queries will be loaded in one table (" + targetDataPath.toString() + ")");
          targetDataPaths = IntStream.of(queryDataPaths.size())
            .mapToObj(s -> targetDataPath)
            .collect(Collectors.toList());
        } else {
          targetDataPaths = queryDataPaths
            .stream()
            .map(s -> DataPaths.childOf(targetDataPath, s.getName()))
            .collect(Collectors.toList());
        }
      }

      TransferProperties transferProperties = CliOptions.getMoveOptions(cliParser);
      List<TransferListener> resultSetListener = new ArrayList<>();
      int i = 0;
      for (Map.Entry<String, DataPath> entry : sourceDataPaths.entrySet()) {

        String queryName = entry.getKey();
        Timer cliTimer = Timer.getTimer("query " + queryName).start();
        DataPath source = entry.getValue();
        DataPath target = targetDataPaths.get(0);
        LOGGER.info("Loading the query (" + queryName + ") from the source (" + sourceDataPath + ") into the table (" + target.toString());


        TransferListener transferListener = Tabulars.move(source, target, transferProperties);
        resultSetListener.add(transferListener);

        LOGGER.info("Response Time for the load of the table (" + source + ") with (" + transferProperties.getTargetWorkerCount() + ") target workers: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
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
}
