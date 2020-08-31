package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.gen.DataGeneration;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.*;


/**
 * load generated data in a table
 */
public class DbTableFill {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableFill.class);


  private static final String LOAD_DEPENDENCIES = "load-parent";
  private static final String DATA_URI_PATTERNS = "DataUriPattern...";
  private static final String AUTO = "auto";


  public static void run(CliCommand cliCommand, String[] args) {


    cliCommand
      .setDescription(Strings.multiline(
        "Load generated data into one or more tables.",
        "This command select the tables to be loaded",
        "You should use a data definition file to define a data generation behaviors that is not automatic."))
      .addExample(Strings.multiline("To load the tables `D_TIME` from the datastore `sqlite` with data:",
        CliUsage.getFullChainOfCommand(cliCommand) + AUTO + " D_TIME@sqlite"))
      .addExample(Strings.multiline("To load the table `D_TIME` with the data definition file `D_TIME--datagen.yml` present in the current directory:",
        CliUsage.getFullChainOfCommand(cliCommand) + "D_TIME D_TIME@datastore"))
      .addExample(Strings.multiline("To load all the tables that have a data definition file in the current directory:",
        CliUsage.getFullChainOfCommand(cliCommand) + "* @datastore"));
    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("One or more data URI patterns (Example: table@database, glob@datastore or table--datadef.yml@database)")
      .setMandatory(true);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("If set, it will not throw an error if a table, file is not found")
      .setDefaultValue(false);
    cliCommand.flagOf(LOAD_DEPENDENCIES)
      .setDescription("If this flag is present, the dependencies of the selected tables (ie parent/foreign tables) will be also filled with data")
      .setDefaultValue(false);
    cliCommand.optionOf(ROWS);

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Boolean loadParent = cliParser.getBoolean(LOAD_DEPENDENCIES);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final long totalNumberOfRows = cliParser.getInteger(ROWS).longValue();
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final List<String> dataUriPatterns = cliParser.getStrings(DATA_URI_PATTERNS);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultDataStoreVault();
      }

      Map<DataStore, List<DataPath>> dataPathsByDataStores = DbStatic.collectDataPathsByDataStore(tabular, dataUriPatterns, notStrictRun, cliCommand);

      Timer cliTimer = Timer.getTimer("Fill tables").start();
      Map<DataStore, List<DataPath>> dataPathsLoadedByDataStore = new HashMap<>();
      for (DataStore dataStore : dataPathsByDataStores.keySet()) {

        List<DataPath> dataPathsByDataStore = dataPathsByDataStores.get(dataStore);
        String withOrWithoutParent = "without the dependencies (the parent/foreign table)";
        if (loadParent) {
          withOrWithoutParent = "with the dependencies (the parent/foreign table)";
        }
        LOGGER.info("Starting filling the tables for the data store " + dataStore + " " + withOrWithoutParent);
        List<TransferListener> transferListeners = DataGeneration.of()
          .addTables(dataPathsByDataStore, totalNumberOfRows)
          .loadDependencies(loadParent)
          .load();
        List<DataPath> targetLoaded = transferListeners.stream().map(t -> t.getTransferSourceTarget().getTargetDataPath()).collect(Collectors.toList());
        dataPathsLoadedByDataStore.put(dataStore, targetLoaded);

      }
      cliTimer.stop();

      LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
      LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");
      LOGGER.info("");
      LOGGER.info("Calculating the size of the tables loaded ...");

      DataPath tablesFilled = tabular.getDataPath("tables_filled")
        .getOrCreateDataDef()
        .addColumn("DataStore")
        .addColumn("Table")
        .addColumn("Size")
        .getDataPath();

      try (InsertStream insertStream = Tabulars.getInsertStream(tablesFilled)) {
        List<DataStore> dataStores = new ArrayList<>(dataPathsLoadedByDataStore.keySet());
        Collections.sort(dataStores);
        for (DataStore dataStore : dataStores) {
          List<DataPath> dataPathsByDataStoreLoaded = dataPathsByDataStores.get(dataStore);
          Collections.sort(dataPathsByDataStoreLoaded);
          for (DataPath dataPath : dataPathsByDataStoreLoaded) {
            insertStream.insert(dataStore.getName(), dataPath.getName(), Tabulars.getSize(dataPath));
          }
        }
      }

      LOGGER.info("The following tables were loaded:");
      Tabulars.print(tablesFilled);

      LOGGER.info("Success ! No errors were seen.");

    }

  }


}
