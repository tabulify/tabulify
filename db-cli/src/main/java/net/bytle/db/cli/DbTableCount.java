package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


public class DbTableCount {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableCount.class);
  ;
  private static final String DATA_URI_PATTERNS = "DataUriPattern...";


  public static void run(CliCommand cliCommand, String[] args) {


    // Create the parser
    cliCommand.setDescription("Count the number of tables");
    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("one or more data URI glob pattern (Example: `*@datastore`)");
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error for minor problem (example if a table was not found with a pattern) ")
      .setDefaultValue(false);

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValueArg = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> dataUriPatterns = cliParser.getStrings(DATA_URI_PATTERNS);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValueArg != null) {
        tabular.setDataStoreVault(storagePathValueArg);
      } else {
        tabular.withDefaultDataStoreVault();
      }

      int count = dataUriPatterns.stream()
        .mapToInt(p->{
          List<DataPath> dataPaths = tabular.select(p);
          if (dataPaths.size()==0){
            String msg = "The data uri pattern ("+p+") has selected no table (no data paths)";
            if (notStrictRunArg) {
              LOGGER.warn(msg);
            } else {
              LOGGER.error(msg);
              System.exit(1);
            }
          }
          return dataPaths.size();
        })
        .sum();

      System.out.println(count + " tables (data paths) were found");

      LOGGER.info("Bye !");

    }

  }


}

