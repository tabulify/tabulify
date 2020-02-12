package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;

public class DbTableCreate {
  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableCreate.class);

  private static final String DATADEF_URI_PATTERN = "DataDefUriPattern...";

  public static void run(CliCommand cliCommand, String[] args) {

    // The command
    cliCommand.setDescription("Create table(s) from data definition file(s)");
    cliCommand.argOf(DATADEF_URI_PATTERN)
      .setDescription("one or more data definition Uri patterns (Example: `*--datadef.yml@database`)")
      .setMandatory(true);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error for minor problem (example if a data def was not found with a pattern) ")
      .setDefaultValue(false);

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValueArg = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> dataDefUriPatterns = cliParser.getStrings(DATADEF_URI_PATTERN);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValueArg != null) {
        tabular.setDataStoreVault(storagePathValueArg);
      } else {
        tabular.withDefaultStorage();
      }

      // Get the data path
      List<DataPath> dataPaths = dataDefUriPatterns
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
        .collect(Collectors.toList());


      Timer cliTimer = Timer.getTimer("Table Creation").start();

      int created = dataPaths.stream().mapToInt(dp -> {
        if (Tabulars.exists(dp)) {
          String msg = "Table (" + dp.getName() + " exists already in the data store (" + dp.getDataStore().getName() + ") and was not created";
          if (notStrictRunArg) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
        } else {
          Tabulars.create(dp);
          LOGGER.info("Table (" + dp + ") was created");
        }
        return 1;
      }).sum();
      cliTimer.stop();

      LOGGER.info("On a total of (" + dataPaths.size() + ") tables, (" + created + ") were created");
      LOGGER.info("Response Time for the creation of the tables : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
      LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


      LOGGER.info("Success ! No errors were seen.");

    }
  }

}
