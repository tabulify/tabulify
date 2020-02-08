package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.*;


/**
 * <p>
 * main class to load the result of a Query into a table
 */
public class DbQueryTransfer {


  private static final Logger LOGGER = LoggerFactory.getLogger(DbQueryTransfer.class);
  ;

  public static void run(CliCommand command, String[] args) {

    // Command
    command
      .setDescription("Transfer the result of query from one source database to a target database")
      .addExample(Strings.multiline("Transfer the result of the query `top10product` from sqlite to the table `top10product` of sql server",
        CliUsage.getFullChainOfCommand(command) + " top10product.sql@sqlite @sqlserver "
      ));
    command.optionOf(DATASTORE_VAULT_PATH);
    command.argOf(Words.SOURCE_DATA_URI)
      .setDescription("A query Uri pattern (fileGlobPattern.sql@dataStore) that defines the queries to transfer")
      .setMandatory(true);
    command.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a query is not found ")
      .setDefaultValue(false);
    command.argOf(TARGET_DATA_URI)
      .setDescription("A target data Uri ([name]@dataStore) (if the target name is not present, it will be the name of the query file)")
      .setMandatory(true);
    DbStaticTransfersOptions.addTransferOptions(command);

    // Create the parser and get the args
    CliParser cliParser = Clis.getParser(command, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String sourceUriPatternArg = cliParser.getString(SOURCE_DATA_URI);
    final String targetUriArg = cliParser.getString(TARGET_DATA_URI);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final TransferProperties transferProperties = DbStaticTransfersOptions.getTransferProperties(cliParser);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      // Args
      List<DataPath> queryDataPaths = tabular.select(sourceUriPatternArg);
      DataPath targetDataPath = tabular.getDataPath(targetUriArg);


      // Transfer
      switch (queryDataPaths.size()) {
        case 0:
          String msg = "No query found";
          if (notStrictRun) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
          break;

        default:

          List<TransferListener> resultSetListener  = TransferManager.of()
            .addTransfers(queryDataPaths, targetDataPath)
            .setTransferProperties(transferProperties)
            .start();

          int exitStatus = resultSetListener.stream().mapToInt(TransferListener::getExitStatus).sum();
          if (exitStatus != 0) {
            LOGGER.error("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(exitStatus);
          } else {
            LOGGER.info("Success ! No errors were seen.");
          }

      }
    }

  }
}
