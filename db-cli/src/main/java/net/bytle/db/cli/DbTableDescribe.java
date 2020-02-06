package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.model.DataDefs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


public class DbTableDescribe {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableDescribe.class);
  ;
  private static final String DATA_URI_PATTERNS = "DataUriPattern...";


  public static void run(CliCommand cliCommand, String[] args) {

    // The command
    cliCommand.setDescription("Show the structure of a table");
    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("One ore more table data uri (@database[/schema]/table")
      .setMandatory(true);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error for minor problem (example if a table was not found with a pattern) ")
      .setDefaultValue(false);

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> dataUriPatterns = cliParser.getStrings(DATA_URI_PATTERNS);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      List<DataPath> dataPaths = dataUriPatterns
        .stream().flatMap(p -> {
          List<DataPath> paths = tabular.select(p);
          if (paths.size() == 0) {
            String msg = "The data def uri pattern (" + p + ") has selected no data def files.";
            if (notStrictRunArg) {
              LOGGER.warn(msg);
            } else {
              LOGGER.error(msg);
              System.exit(1);
            }
          }
          return paths.stream();
        })
        .sorted()
        .collect(Collectors.toList());

      if (dataPaths.size() == 0) {
        String msg = "The data def uri patterns (" + dataUriPatterns + ") have selected no tables.";
        if (notStrictRunArg) {
          LOGGER.warn(msg);
          System.exit(0);
        } else {
          LOGGER.error(msg);
          System.exit(1);
        }
      } else {
        DataPath columns = DataDefs.getColumnsDataPath(tabular, dataPaths);
        Tabulars.print(columns);
      }
      System.out.println();
    }


    LOGGER.info("Bye !");


  }


}
