package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


public class DbForeignKeyCount {

  private static final Log LOGGER = Db.LOGGER_DB_CLI;
  private static final String DATA_URI_PATTERNS = "DataUriPattern...";


  public static void run(CliCommand cliCommand, String[] args) {

    cliCommand
      .setDescription("Count links (foreign keys)");

    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("One or more name data uri pattern (ie glob@datastore)")
      .setMandatory(true);

    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> dataUris = cliParser.getStrings(DATA_URI_PATTERNS);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultDataStoreVault();
      }

      // Collect the foreign keys
      List<ForeignKeyDef> foreignKeys = dataUris.stream()
        .flatMap(du -> tabular.select(du).stream())
        .flatMap(dp -> dp.getDataDef().getForeignKeys().stream())
        .collect(Collectors.toList());

      System.out.println();
      if (foreignKeys.size() == 0) {

        System.out.println("No foreign key found");

      } else {

        System.out.println(foreignKeys.size() + " ForeignKeys");

      }
      System.out.println();
      LOGGER.info("Bye !");

    }

  }

}
