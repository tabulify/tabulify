package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dbs {

  private static final Logger LOGGER = LoggerFactory.getLogger(Dbs.class);

  public static Map<DataStore, List<DataPath>> collectDataPaths(Tabular tabular, List<String> dataUriPatterns, Boolean notStrictRun, CliCommand cliCommand){
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
}
