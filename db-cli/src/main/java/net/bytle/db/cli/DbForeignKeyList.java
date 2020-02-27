package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


public class DbForeignKeyList {

  private static final Log LOGGER = Db.LOGGER_DB_CLI;
  private static final String DATAURI_PATTERNS = "DataUriPattern...";
  protected static final String SHOW_COLUMN = "c";


  public static void run(CliCommand cliCommand, String[] args) {

    String description = "List the foreign keys";

    // Create the command
    cliCommand.setDescription(description);
    cliCommand.argOf(DATAURI_PATTERNS)
      .setDescription("One or more name data uri pattern (ie glob@datastore")
      .setMandatory(true);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(SHOW_COLUMN).setDescription("Show also the columns if present");

    // Parse and control the args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> dataUris = cliParser.getStrings(DATAURI_PATTERNS);

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
        .flatMap(dp -> dp.getOrCreateDataDef().getForeignKeys().stream())
        .collect(Collectors.toList());


      if (foreignKeys.size() == 0) {

        System.out.println("No relation found");

      } else {

        // Sorting them ascending
        Collections.sort(foreignKeys);

        // Creating a table to use the print function
        DataPath foreignKeysInfo = tabular.getDataPath("foreignKeys")
          .getOrCreateDataDef()
          .addColumn("Id", Types.INTEGER)
          .addColumn("Child/Foreign Table")
          .addColumn("<-")
          .addColumn("Parent/Primary Table")
          .addColumn("From Foreign Key")
          .getDataPath();

        Boolean showColumns = cliParser.getBoolean(SHOW_COLUMN);

        try (
          InsertStream insertStream = Tabulars.getInsertStream(foreignKeysInfo)
        ) {
          // Filling the table with data
          Integer fkNumber = 0;
          for (ForeignKeyDef foreignKeyDef : foreignKeys) {
            final String[] nativeColumns = foreignKeyDef.getChildColumns().stream()
              .map(ColumnDef::getColumnName)
              .collect(Collectors.toList())
              .toArray(new String[foreignKeyDef.getChildColumns().size()]);
            final String childCols = String.join(",", nativeColumns);
            final String[] pkColumns = foreignKeyDef.getForeignPrimaryKey().getColumns().stream()
              .map(ColumnDef::getColumnName)
              .collect(Collectors.toList())
              .toArray(new String[foreignKeyDef.getForeignPrimaryKey().getColumns().size()]);

            String parentCols = String.join(",", pkColumns);
            fkNumber++;

            insertStream.insert(
              fkNumber,
              foreignKeyDef.getTableDef().getDataPath().getName() + (showColumns ? " (" + childCols + ")" : ""),
              "<-",
              foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath().getName() + (showColumns ? " (" + parentCols + ")" : ""),
              foreignKeyDef.getName()
            );

          }
        }

        // Printing
        System.out.println();
        System.out.println("ForeignKeys:");
        Tabulars.print(foreignKeysInfo);

        if (!showColumns) {
          System.out.println();
          System.out.println("Tip: You can show the columns by adding the c flag.");
        }
        // In the test we run it twice,
        // we will then insert the data twice
        // We need to suppress the data
        Tabulars.delete(foreignKeysInfo);


      }
      System.out.println();
      LOGGER.info("Bye !");

    }
  }

}
