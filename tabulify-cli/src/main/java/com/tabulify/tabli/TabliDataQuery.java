package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.sql.SqlPlusLexer;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.uri.DataUri;
import net.bytle.log.Log;
import net.bytle.timer.Timer;
import net.bytle.type.time.Timestamp;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.DATA_SELECTORS;
import static com.tabulify.tabli.TabliWords.OUTPUT_DATA_URI;


public class TabliDataQuery {


  public static List<DataPath> run(Tabular tabular, CliCommand cliCommand) {

    cliCommand.setDescription(
      "The query command allows performance test.",
      "",
      "This command will execute one or multiple queries, fetch the data (ie get the data locally) and returns the performance metrics",
      "",
      "A query is an executable statement that returns data.",
      "In Sql, if the statement is a `select` statement it will be executed, otherwise the query will be created for a table or a view.",
      "This means that you can also use this command to measure the fetch of data on your network.",
      "",
      "",
      "Note: If you want to:",
      "  * show the data result of a query execution, the `print`, `head`, `tail` command is recommended",
      "  * transfer/load the result of a query, you should use the `transfer` command",
      "  * shows the structure of a query, you should use the `struct` command"

    );
    cliCommand.addArg(DATA_SELECTORS)
      .setDescription("One or several data selectors that selects data resource (query, view, table)")
      .setMandatory(true);

    // Examples
    cliCommand.addExample(
      "Execute all the queries written in the sql files that begins with `dim` in the current directory (ie `local` connection)",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + " (dim*.sql@local)@sqlite",
      CliUsage.CODE_BLOCK
    );
    cliCommand.addExample(
      "Execute the query written in the file `Query1.sql` against the `sqlite` connection and execute the query written in the file `Query2.sql` against the `oracle` connection ",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + " (Query1.sql)@sqlite (Query2.sql)@oracle",
      CliUsage.CODE_BLOCK
    );
    String projectWithQueries = "project/withQueries";
    cliCommand.addExample(
      "Execute all sql files present in the local directory `" + projectWithQueries + "` against the `postgres` data store and store the result in the `perf` table.",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + " " + OUTPUT_DATA_URI + " perf@postgres (" + projectWithQueries + "/*.sql)@postgres",
      CliUsage.CODE_BLOCK
    );

    // Parse and Args
    CliParser cliParser = cliCommand.parse();
    final List<DataUri> dataSelectors = cliParser.getStrings(DATA_SELECTORS)
      .stream()
      .map(ds -> DataUri.createFromString(tabular, ds))
      .collect(Collectors.toList());


    // Arguments checks
    // Collecting the queries/view
    List<DataPath> dataPathsToExecute = new ArrayList<>();
    for (DataUri dataSelector : dataSelectors) {

      Connection connection = dataSelector.getConnection();

      if (dataSelector.isScriptSelector()) {
        List<DataPath> queryFileDataSelectors = tabular.select(dataSelector.getScriptUri());
        if (queryFileDataSelectors.size() == 0) {
          TabliLog.LOGGER_TABLI.warning("No data resources found with the data selector (" + dataSelector.getScriptUri() + ")");
        }
        for (DataPath selectDataPathsFromSelector : queryFileDataSelectors) {
          if (Tabulars.isDocument(selectDataPathsFromSelector) && selectDataPathsFromSelector instanceof FsDataPath) {
            FsDataPath fsDataPath = (FsDataPath) selectDataPathsFromSelector;
            List<String> selectStatements;
            try(SqlPlusLexer fromPath = SqlPlusLexer.createFromPath(fsDataPath.getAbsoluteNioPath())) {
              selectStatements = fromPath.getSqlStatements();
            }
            if (selectStatements.isEmpty()) {
              tabular.warningOrTerminateIfStrict("No query found in the file (" + fsDataPath + ")");
            } else {
              for (int i = 0; i < selectStatements.size(); i++) {
                String queryName = fsDataPath.getNioPath().getFileName().toString() + "_" + (i + 1);
                DataPath queryDataPath = connection.createScriptDataPath(tabular.getAndCreateRandomMemoryDataPath()
                  .setLogicalName(queryName)
                  .setContent(selectStatements.get(0))
                );
                dataPathsToExecute.add(queryDataPath);
              }
            }
          }
        }
      } else {
        List<DataPath> select = tabular.select(dataSelector);
        if (select.isEmpty()) {
          TabliLog.LOGGER_TABLI.warning("No data resources found with the data selector (" + dataSelector.getScriptUri() + ")");
        } else {
          dataPathsToExecute.addAll(select);
        }
      }

      TabliLog.LOGGER_TABLI.info(dataPathsToExecute.size() + " data resources were found for the data selector (" + dataPathsToExecute + ")");

    }

    DataPath feedbackDataPath = tabular.getMemoryDataStore().getDataPath("executions")
      .getOrCreateRelationDef()
      .addColumn("Query Name", Types.VARCHAR)
      .addColumn("Start Time", Types.TIMESTAMP)
      .addColumn("End Time", Types.TIMESTAMP)
      .addColumn("Latency (ms)", Types.INTEGER)
      .addColumn("Row Count", Types.INTEGER)
      .addColumn("Error", Types.VARCHAR)
      .addColumn("Error Message", Types.VARCHAR)
      .getDataPath();

    TabliLog.LOGGER_TABLI.info("Executing the " + dataPathsToExecute.size() + " queries (view, fetch, ...)");
    if (dataPathsToExecute.size() == 0) {
      tabular.warningOrTerminateIfStrict("No data resource query found");
    } else {
      try (
        InsertStream exeInput = feedbackDataPath.getInsertStream()
      ) {

        for (DataPath dataPath : dataPathsToExecute) {

          Timer cliTimer = Timer.create("execute").start();
          Long rowCount = null;
          String status = "";
          String message = "";
          try {
            rowCount = dataPath.getCount();
          } catch (Exception e) {
            status = "Err";
            message = Log.onOneLine(e.getMessage());
            tabular.warningOrTerminateIfStrict(e.getMessage());
          }

          cliTimer.stop();

          exeInput.insert(dataPath.getLogicalName(),
            Timestamp.createFromInstant(cliTimer.getStartTime()).toSqlTimestamp(),
            Timestamp.createFromInstant(cliTimer.getEndTime()).toSqlTimestamp(),
            cliTimer.getResponseTimeInMilliSeconds(),
            rowCount,
            status,
            message);

        }
      }

    }
    return Collections.singletonList(feedbackDataPath);
  }
}
