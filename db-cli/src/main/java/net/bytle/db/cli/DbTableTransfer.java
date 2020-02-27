package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.log.Log;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * transfer a table into a db
 */
public class DbTableTransfer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableTransfer.class);


  public static void run(CliCommand cliCommand, String[] args) {


    cliCommand.setDescription(Strings.multiline(
      "Transfer one or more tables from a database to another.",
      "If the target table does not exist, it will be created."
    ));
    cliCommand.argOf(SOURCE_DATA_URI)
      .setDescription("A data uri glob patterns that define the table(s) to transfer")
      .setMandatory(true);
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A data URI that defines the destination (Example: [table]@datastore) If the data uri has no name, the name will be the name of the source.")
      .setMandatory(false);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a table is not found with the source table Uri")
      .setDefaultValue(false);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    DbStaticTransfersOptions.addTransferOptions(cliCommand);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final String sourceUriPatternArg = cliParser.getString(SOURCE_DATA_URI);
    final String targetUriArg = cliParser.getString(TARGET_DATA_URI);
    final TransferProperties transferProperties = DbStaticTransfersOptions.getTransferProperties(cliParser);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultDataStoreVault();
      }


      // Args
      List<DataPath> dataPaths = tabular.select(sourceUriPatternArg);
      DataPath targetDataPath = tabular.getDataPath(targetUriArg);

      // Transfer
      switch (dataPaths.size()) {
        case 0:
          String msg = "No data path (table) found";
          if (notStrictRun) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
          break;

        default:

          LOGGER.info("Processing the request");
          Timer totalCliTimer = Timer.getTimer("total").start();

          List<TransferListener> resultSetListeners = TransferManager.of()
            .addTransfers(dataPaths, targetDataPath)
            .setTransferProperties(transferProperties)
            .start();

          Collections.sort(resultSetListeners);

          DataPath result = tabular.getDataPath("result_table_transfer")
            .getOrCreateDataDef()
            .addColumn("Source Table Name", Types.VARCHAR)
            .addColumn("Target Table Name", Types.VARCHAR)
            .addColumn("Latency (ms)", Types.INTEGER)
            .addColumn("Row Count", Types.INTEGER)
            .addColumn("Error", Types.VARCHAR)
            .addColumn("Message", Types.VARCHAR)
            .getDataPath();

          try (InsertStream insertStream = Tabulars.getInsertStream(result)) {
            resultSetListeners.forEach(l -> {
                insertStream.insert(
                  l.getSourceTarget().getSourceDataPath().toString(),
                  l.getSourceTarget().getTargetDataPath().toString(),
                  l.getResponseTime(),
                  l.getRowCount(),
                  l.getExitStatus() != 0 ? l.getExitStatus() : "",
                  Log.onOneLine(l.getErrorMessage())
                );
              }
            );
          }

          totalCliTimer.stop();

          LOGGER.info("Response Time to transfer the data: " + totalCliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
          LOGGER.info("       Ie (" + totalCliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");

          int errors = resultSetListeners.stream().mapToInt(TransferListener::getExitStatus).sum();
          if (errors > 0) {
            LOGGER.error(errors + " errors during table transfer executions were seen");
            System.exit(1);
          } else {
            LOGGER.info("Success !");
          }

      }
    }

  }
}
