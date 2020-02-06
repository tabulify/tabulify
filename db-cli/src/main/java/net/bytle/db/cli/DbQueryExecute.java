package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.log.Log;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static net.bytle.db.cli.Words.*;


public class DbQueryExecute {


  private static final Logger LOGGER = LoggerFactory.getLogger(DbQueryExecute.class);
  private static final String QUERY_URI_PATTERNS = "QueryUriPattern...";


  public static void run(CliCommand cliCommand, String[] args) {

    cliCommand.setDescription(Strings.multiline(
      "Execute one or several queries. ",
      "  * For one query, the data is shown.",
      "  * For multiple queries, the performance result is shown."
    ));
    cliCommand.argOf(QUERY_URI_PATTERNS)
      .setDescription("One or several query uri glob pattern that defines a query (inline or in a sql file) and defines the data store")
      .setMandatory(true);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a query is not found ")
      .setDefaultValue(false);
    cliCommand.optionOf(OUTPUT_DATA_URI)
      .setDescription("defines the destination of the output as a data uri. If set the output will be also saved into this data store (Example: perf@mysql or perf.csv@file)");

    // Examples
    cliCommand.addExample(Strings.multiline(
      "Execute all the queries written in the sql files that begins with `dim`",
      Words.CLI_NAME + " " + cliCommand.getName() + " dim*.sql@sqlite"
    ));
    cliCommand.addExample(Strings.multiline(
      "Execute the query written in the file `Query1.sql` against the `sqlite` datastore and ...",
      "execute the query written in the file `Query2.sql` against the `oracle` datastore ",
      Words.CLI_NAME + " " + cliCommand.getName() + " Query1.sql@sqlite Query2.sql@oracle"
    ));
    cliCommand.addExample(Strings.multiline(
      "Execute an inline query against the `sqlite` datastore",
      Words.CLI_NAME + " " + cliCommand.getName() + " \"select year, count(1) from sales group by year@sqlite\""
    ));
    cliCommand.addExample(Strings.multiline(
      "Execute all sql files present in the directory `directory/withQueries` against the `postgres` data store and store the result in the `perf` table.",
      Words.CLI_NAME + " " + cliCommand.getName() + " " + CliParser.PREFIX_LONG_OPTION + OUTPUT_DATA_URI + "perf@postgres ./directory/withQueries/*.sql@postgres"
    ));

    // Parse and Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final List<String> queryUriArgs = cliParser.getStrings(QUERY_URI_PATTERNS);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final String outputDataUri = cliParser.getString(OUTPUT_DATA_URI);

    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      // Arguments checks
      // Collecting the queries
      List<DataPath> queries = new ArrayList<>();
      for (int i = 0; i < queryUriArgs.size(); i++) {
        String queryUriArg = queryUriArgs.get(i);
        List<DataPath> dataPaths = tabular.select(queryUriArg);
        queries.addAll(dataPaths);
        if (dataPaths.size() == 0) {
          String msg = "The query uri (" + queryUriArg + ") is not a query nor a pattern that select files/table";
          if (notStrictRun) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            CliUsage.print(cliCommand);
            System.exit(1);
          }
        }
      }
      DataPath outputDataPath = null;
      if (outputDataUri != null) {
        outputDataPath = tabular.getDataPath(outputDataUri);
        if (Tabulars.exists(outputDataPath)) {
          String msg = "The output table (" + outputDataUri + ") exists already.";
          if (notStrictRun) {
            LOGGER.error(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
        }
      }


      LOGGER.info("{} queries were found", queries.size());
      LOGGER.info("Executing {} queries", queries.size());
      switch (queries.size()) {
        case 0:

          String msg = "No query found";
          if (notStrictRun) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }

          break;

        case 1:

          // Prep
          Timer cliTimer = Timer.getTimer("execute").start();

          // Begin output
          DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
          print(outputDataPath, queries.get(0));
          DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);

          // Feedback
          cliTimer.stop();
          LOGGER.info("Response Time to query the data: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
          LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");
          break;

        default:

          DataPath executionTable = tabular.getDataPath("executions")
            .getDataDef()
            .addColumn("Query Name", Types.VARCHAR)
            .addColumn("Start Time", Types.DATE)
            .addColumn("End Time", Types.DATE)
            .addColumn("Latency (ms)", Types.INTEGER)
            .addColumn("Row Count", Types.INTEGER)
            .addColumn("Error", Types.VARCHAR)
            .addColumn("Error Message", Types.VARCHAR)
            .getDataPath();

          int errorCounter = 0;
          try (
            InsertStream exeInput = Tabulars.getInsertStream(executionTable);
          ) {

            for (DataPath dataPath : queries) {

              cliTimer = Timer.getTimer("execute").start();
              Integer rowCount = null;
              String status = "";
              String message = "";
              try {
                rowCount = Tabulars.getSize(dataPath);
              } catch (Exception e) {
                errorCounter++;
                status = "Err";
                message = Log.onOneLine(e.getMessage());
                LOGGER.error(e.getMessage());
                if (!notStrictRun) {
                  System.exit(1);
                }
              }

              cliTimer.stop();
              String description = dataPath.getDescription();
              if (description == null) {
                description = dataPath.getName();
              }
              exeInput.insert(description, cliTimer.getStartTime(), cliTimer.getEndTime(), cliTimer.getResponseTimeInMilliSeconds(), rowCount, status, message);

            }
          }

          print(outputDataPath, executionTable);

          if (errorCounter > 0) {
            System.err.println(errorCounter + " Errors during Query executions were seen");
            System.exit(1);
          }
          break;
      }

      LOGGER.info("Bye !");
    }
  }

  /**
   * Print the output
   *
   * @param outputDataPath
   * @param executionTable
   */
  private static void print(DataPath outputDataPath, DataPath executionTable) {
    System.out.println();
    Tabulars.print(executionTable);
    System.out.println();
    if (outputDataPath != null) {
      Tabulars.copy(executionTable, outputDataPath);
    }
  }

}
