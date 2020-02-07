package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


/**
 *
 */
public class DbTableList {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableList.class);
  private static final String DATA_URI_PATTERN = "DataUriPattern...";


  public static void run(CliCommand cliCommand, String[] args) {

    // Command
    cliCommand.setDescription("Print a list of tables")
      .addExample(Strings.multiline("List all the tables of the current schema of the `oracle` data store",
        CliUsage.getFullChainOfCommand(cliCommand) + "*@oracle"))
      .addExample(Strings.multiline("List all the tables that begins with `D` of the `sqlite` data store",
        CliUsage.getFullChainOfCommand(cliCommand) + "D*@sqlite"));
    cliCommand.argOf(DATA_URI_PATTERN)
      .setDescription("One or more name data uri pattern (ie pattern@datastore")
      .setMandatory(true);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(Words.NO_COUNT)
      .setDescription("suppress the column showing the table count")
      .setShortName("c");
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a table is not found")
      .setDefaultValue(false);

    // Arguments
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> dataUriPatterns = cliParser.getStrings(DATA_URI_PATTERN);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final Boolean noCountColumn = cliParser.getBoolean(Words.NO_COUNT);

// Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      Map<DataStore, List<DataPath>> dataPathsByDataStores = Dbs.collectDataPathsByDataStore(tabular, dataUriPatterns, notStrictRun, cliCommand);

      List<DataStore> dataStores = new ArrayList<>(dataPathsByDataStores.keySet());
      Collections.sort(dataStores);

      DataPath tables = tabular.getDataPath("tables");
      switch (dataStores.size()) {
        case 0:
          String msg = "No data path found (no tables found)";
          if (notStrictRun) {
            LOGGER.warn(msg);
            System.exit(0);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
          break;
        case 1:
          tables
            .getDataDef()
            .addColumn("Table Name");
          if (!noCountColumn) {
            tables.getDataDef().addColumn("Rows Count", Types.INTEGER);
          }
          try (InsertStream insertStream = Tabulars.getInsertStream(tables)) {
            List<DataPath> dataPathsByDataStore = dataPathsByDataStores.entrySet().iterator().next().getValue();
            Collections.sort(dataPathsByDataStore);
            for (DataPath dataPath : dataPathsByDataStore) {
              if (!noCountColumn) {
                Integer count = Tabulars.getSize(dataPath);
                insertStream.insert(dataPath.getName(), count);
              } else {
                insertStream.insert(dataPath.getName());
              }
            }
          }
          break;
        default:
          tables
            .getDataDef()
            .addColumn("Data Store")
            .addColumn("Table Name")
            .getDataPath();
          if (!noCountColumn) {
            tables.getDataDef().addColumn("Rows Count", Types.INTEGER);
          }
          try (InsertStream insertStream = Tabulars.getInsertStream(tables)) {
            for (DataStore dataStore : dataStores) {
              List<DataPath> dataPathsByDataStore = dataPathsByDataStores.get(dataStore);
              Collections.sort(dataPathsByDataStore);
              for (DataPath dataPath : dataPathsByDataStore) {
                if (!noCountColumn) {
                  Integer count = Tabulars.getSize(dataPath);
                  insertStream.insert(dataStore.getName(), dataPath.getName(), count);
                } else {
                  insertStream.insert(dataStore.getName(), dataPath.getName());
                }
              }
            }
          }
          break;
      }
      Tabulars.print(tables);
    }
  }
}
