package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


/**
 *
 */
public class DbSchemaList {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbSchemaList.class);
  private static final String DATA_URI_PATTERNS = "DataUriPattern";


  public static void run(CliCommand cliCommand, String[] args) {

    // Create the Command
    cliCommand.setDescription("List schemas");

    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("One or more schema data uri glob pattern (example: `../*@datastore`) ")
      .setMandatory(true);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error for minor problem (example if a schema was not found) ")
      .setDefaultValue(false);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final List<String> dataUriPatterns = cliParser.getStrings(DATA_URI_PATTERNS);
    final Path storagePathValueArg = cliParser.getPath(DATASTORE_VAULT_PATH);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValueArg != null) {
        tabular.setDataStoreVault(storagePathValueArg);
      } else {
        tabular.withDefaultDataStoreVault();
      }

      // Target
      List<DataPath> allSelectedDataPaths = new ArrayList<>();
      for (String globPattern : dataUriPatterns) {
        List<DataPath> selectedDataPaths = tabular.select(globPattern);
        if (selectedDataPaths.size() == 0) {
          String msg = "The schema data uri pattern has selected no schema (no data paths)";
          if (notStrictRunArg) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
        }
        selectedDataPaths.forEach(d -> {
          if (!Tabulars.isContainer(d)) {
            String msg = "The schema data uri pattern has selected a data path (" + d + ") that is not a schema (not a container)";
            if (notStrictRunArg) {
              LOGGER.warn(msg);
            } else {
              LOGGER.error(msg);
              System.exit(1);
            }
          }
        });
        allSelectedDataPaths.addAll(selectedDataPaths);
      }

      if (allSelectedDataPaths.size() == 0) {
        String msg = "The data uri patterns (" + dataUriPatterns + ") has selected no schema (no data path)";
        if (notStrictRunArg) {
          LOGGER.warn(msg);
        } else {
          LOGGER.error(msg);
          System.exit(1);
        }
      } else {
        DataPath output = tabular.getDataPath("output")
          .getDataDef()
          .addColumn("Name")
          .addColumn("NumberOfChildren")
          .getDataPath();
        Tabulars.create(output);

        Collections.sort(allSelectedDataPaths);

        try (InsertStream insertStream = Tabulars.getInsertStream(output)) {
          allSelectedDataPaths.forEach(dataPath -> {
            int children = Tabulars.getChildren(dataPath).size();
            insertStream.insert(dataPath.getName(), children);
          });
        }

        Tabulars.print(output);
      }
      LOGGER.info("Bye !");


    }


  }
}
