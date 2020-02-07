package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytle.db.cli.Words.*;


public class DbTableDrop {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableDrop.class);

  private static final String DATA_URI_PATTERNS = "dataUriPattern...";

  public static void run(CliCommand cliCommand, String[] args) {

    // Create the parser
    cliCommand
      .setDescription(Strings.multiline("Drop table(s)",
        "!!!!!! Warning !!!!!!",
        "Due to the data uri concept, if the data uri pattern is a valid one and defines other data type such as one or more files, this command will delete them.",
        "!!!!!!!!!!!!!!!!!!!!!"))
      .addExample(Strings.multiline("To drop the tables D_TIME and F_SALES:",
        CliUsage.getFullChainOfCommand(cliCommand) + "D_TIME@datastore F_SALES@datastore"))
      .addExample(Strings.multiline("To drop only the table D_TIME with force (ie deleting the foreign keys constraint):" +
        CliUsage.getFullChainOfCommand(cliCommand) + CliParser.PREFIX_LONG_OPTION + FORCE + "D_TIME@database"))
      .addExample(Strings.multiline("To drop all dimension tables that begins with (D_):",
        CliUsage.getFullChainOfCommand(cliCommand) + " D_*@datastore"))
      .addExample(Strings.multiline("To drop all tables from the current schema:",
        CliUsage.getFullChainOfCommand(cliCommand) + " *@database"));
    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("One or more data URI glob pattern (Example: tableName@datastore)")
      .setMandatory(true);
    cliCommand.flagOf(FORCE)
      .setDescription("if set, the foreign keys referencing the tables to drop will be dropped");
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a table is not found")
      .setDefaultValue(false);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final Boolean withForce = cliParser.getBoolean(FORCE);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final List<String> dataUriPatterns = cliParser.getStrings(DATA_URI_PATTERNS);


    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

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

      // Doing the work
      for (DataStore dataStore : dataPathsByDataStores.keySet()) {
        List<DataPath> dataPathsByDataStore = dataPathsByDataStores.get(dataStore);
        for (DataPath dataPathToDrop : ForeignKeyDag.get(dataPathsByDataStore).getDropOrderedTables()) {

          List<DataPath> referenceDataPaths = Tabulars.getReferences(dataPathToDrop);
          for (DataPath referenceDataPath : referenceDataPaths) {
            if (!dataPathsByDataStore.contains(referenceDataPath)) {
              if (withForce) {

                List<ForeignKeyDef> droppedForeignKeys = Tabulars.dropOneToManyRelationship(referenceDataPath, dataPathToDrop);
                droppedForeignKeys
                  .forEach(fk -> LOGGER.warn("ForeignKey (" + fk.getName() + ") was dropped from the table (" + fk.getTableDef().getDataPath() + ")"));

              } else {

                LOGGER.error("The table (" + referenceDataPath + ") is referencing the table (" + dataPathToDrop + ") and is not in the tables to drop");
                LOGGER.error("To drop the foreign keys referencing the tables to drop, you can add the force flag (" + CliParser.PREFIX_LONG_OPTION + Words.FORCE + ").");
                LOGGER.error("Exiting");
                System.exit(1);

              }
            }
          }

          Tabulars.drop(dataPathToDrop);
          LOGGER.info("Table (" + dataPathToDrop + ") was dropped.");
        }
      }

      LOGGER.info("Bye !");

    }
  }

}
