package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


public class DbTableShow {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableShow.class);
  private static final String DATA_URI_PATTERNS = "TableUri..";
  protected static final String LIMIT = "limit";
  private static final String HEAD = "head";
  private static final String TAIL = "tail";

  public static void run(CliCommand cliCommand, String[] args) {

    // Command
    cliCommand.setDescription(Strings.multiline("Show the data of a table",
      "This command was designed to render data properly aligned.",
      "This is why there is a limit on the data to render by default.",
      "If you want to get more data, you can always use the `download` or `transfer` command."));
    cliCommand.argOf(DATA_URI_PATTERNS)
      .setDescription("One or more data URI patterns (Example: glob@datastore)")
      .setMandatory(true);
    cliCommand.addExample(Strings.multiline("Show the data of the table `sales` from the data store `sqlite`:",
      CliUsage.getFullChainOfCommand(cliCommand) + "sales@sqlite"));
    cliCommand.addExample(Strings.multiline("Show the last 100 rows of the table `time` from the data store `postgres`:",
      CliUsage.getFullChainOfCommand(cliCommand) + CliParser.PREFIX_LONG_OPTION + TAIL + " " + CliParser.PREFIX_LONG_OPTION + LIMIT + " 100 sales@postgres"));
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.optionOf(LIMIT)
      .setDescription("Limit the number of rows returned")
      .setDefaultValue(10);
    cliCommand.flagOf(HEAD)
      .setDescription("Select the number rows from the head")
      .setDefaultValue(false);
    cliCommand.flagOf(TAIL)
      .setDescription("Select the number of rows from the tail")
      .setDefaultValue(false);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("If set, it will not throw an error but a warning if the command may continue (example: if a table is not found)")
      .setDefaultValue(false);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final Integer limit = cliParser.getInteger(LIMIT);
    final List<String> tableURIs = cliParser.getStrings(DATA_URI_PATTERNS);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final Boolean tail = cliParser.getBoolean(TAIL);
    final Boolean head = cliParser.getBoolean(TAIL);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      List<DataPath> tableDefList = Dbs.collectDataPaths(tabular, tableURIs, notStrictRun, cliCommand);
      // Timer
      Timer cliTimer = Timer.getTimer("execute").start();


      if (tableDefList.size() == 0) {
        String msg = "No data path found (no tables found)";
        if (notStrictRun) {
          LOGGER.warn(msg);
          System.exit(0);
        } else {
          LOGGER.error(msg);
          System.exit(1);
        }
      } else {


        for (DataPath dataPath : tableDefList) {


          System.out.println("Data from the table (" + dataPath.getName() + "): ");
          DataPath subset = Tabulars.getSubSet(dataPath,limit, Tabulars.TAIL);

          Tabulars.print(subset);


        }


        // Feedback
        cliTimer.stop();
        LOGGER.info("Response Time to query the data: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");

        System.out.println();
        LOGGER.info("Bye !");

      }

    }


  }
}
