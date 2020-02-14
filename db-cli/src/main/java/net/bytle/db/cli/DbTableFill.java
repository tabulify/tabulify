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
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


/**
 * load generated data in a table
 */
public class DbTableFill {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableFill.class);


  static final String NUMBER_OF_ROWS_OPTION = "rows";
  private static final String LOAD_DEPENDENCIES = "load-parent";
  private static final String DATA_URI_PATTERNS = "DataUriPattern...";


  public static void run(CliCommand cliCommand, String[] args) {


    cliCommand
      .setDescription(Strings.multiline(
        "Load generated data into one or more tables",
        "By default, the data would be randomly generated.",
        "You should use a data definition file to define a data generation behaviors that is not random."))
      .addExample(Strings.multiline("To load the tables from the database `sqlite` with random data:",
        CliUsage.getFullChainOfCommand(cliCommand) + "random  *@sqlite"))
      .addExample(Strings.multiline("To load the tables `D_TIME` from the datastore `sqlite` with random data:",
        CliUsage.getFullChainOfCommand(cliCommand) + "random  D_TIME@sqlite"))
      .addExample(Strings.multiline("To load the table `D_TIME` with the data definition file `D_TIME--datadef.yml` present in the current directory:",
        CliUsage.getFullChainOfCommand(cliCommand) + "D_TIME--datadef.yml  D_TIME@datastore"))
      .addExample(Strings.multiline("To load all the tables that have a data definition file in the current directory:",
        CliUsage.getFullChainOfCommand(cliCommand) + "*--datadef.yml   *@datastore"));
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
    cliCommand.optionOf(NUMBER_OF_ROWS_OPTION)
      .setDescription("This option defines the total number of rows that the table(s) must have. For a number of rows defined by table, you should set it in a datadef file.");

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Boolean loadParent = cliParser.getBoolean(LOAD_DEPENDENCIES);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final Integer totalNumberOfRows = cliParser.getInteger(NUMBER_OF_ROWS_OPTION);
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
        List<DataPath> dataPathsLoaded = DataGeneration.of()
          .addTables(dataPathsByDataStore, totalNumberOfRows)
          .loadDependencies(loadParent)
          .load();
        dataPathsLoadedByDataStore.put(dataStore, dataPathsLoaded);

      }
      cliTimer.stop();

      LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
      LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");
      LOGGER.info("");
      LOGGER.info("Calculating the size of the tables loaded ...");

      DataPath tablesFilled = tabular.getDataPath("tables_filled")
        .getDataDef()
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
