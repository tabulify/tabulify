package net.bytle.db.cli;

import net.bytle.cli.*;

import net.bytle.db.resultSetDiff.DataSetDiff;
import net.bytle.db.resultSetDiff.ExecuteQueryThread;
import net.bytle.type.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbQueryDiff {

  private static final Logger LOGGER = Logger.getLogger(DbQueryDiff.class.getPackage().toString());


  public static void run(CliCommand cliCommand, String[] args) {

    // The command
    cliCommand
      .setDescription("Performs a diff between two queries.")
      .addExample(Strings.multiline("Diff between two different queries:",DIFF_COMMAND+" queryFile1.sql@sqlite queryFile2.sql@sqlite"))
      .addExample(Strings.multiline("Diff between two different datastore:",DIFF_COMMAND+" queryFile.sql@sqlite queryFile.sql@postgres"));
    CliWord firstQueryWord = cliCommand.argOf("QueryUri").setDescription("A query URI pattern (Example: dim*.sql@datastore)")
      .setMandatory(true);
    CliWord secondQueryWord = cliCommand.argOf("QueryUri").setDescription("A query URI pattern (Example: dim*.sql@datastore)")
      .setMandatory(true);
    cliCommand.optionOf(OUTPUT_DATA_URI)
      .setDescription("defines the destination of the diff as a data uri");

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    String argFirstQuery = cliParser.getString(firstQueryWord);
    String argSecondQuery = cliParser.getString(secondQueryWord);


    List<Query> queries = new ArrayList<>();
    if (cliArgs.size() == 1) {
      // Diff between two different databases
      Path outputPathArg = cliParser.getPath(OUTPUT_DATA_URI);

      // Source
      String arg1 = cliParser.getString(firstQueryWord);
      if (arg1.toUpperCase().contains("SELECT")) {

        Query query = new Query(arg1);
        query.outputPath(outputPathArg);
        query.name("Query from the command line");
        LOGGER.info("A single query was given as input of the diff.");
        queries.add(query);

      } else {

        Path pathArg1 = null;
        try {
          pathArg1 = Paths.get(arg1);
        } catch (InvalidPathException e) {
          // Not a path nor a sql
          System.err.println("In the first argument mode, the first argument is expected to be an directory, a SQL file or a SQL query containing the SELECT keyword.");
          System.err.println("The below argument is not one of this.");
          System.err.println("   " + arg1);
          CliUsage.print(cliCommand);
          System.exit(1);
        }

        if (!Files.isDirectory(pathArg1)) {

          if (Files.exists(pathArg1)) {

            String sqlQuery = cliParser.getFileContent(arg1, false);
            Query query = new Query(sqlQuery);
            query.outputPath(outputPathArg);
            query.name(pathArg1.getFileName().toString());
            queries.add(query);

          } else {

            System.err.println("The first argument contains a path (" + pathArg1 + ") that doesn't exist.");
            System.exit(1);

          }


        } else {

          Path batchOutputPath = Paths.get(pathArg1.toAbsolutePath().toString(), "DiffBatchResult.csv");


          try {
            Files.newDirectoryStream(pathArg1).forEach(e -> {
              if (e.getFileName().toString().endsWith(".sql")) {
                String queryString = cliParser.getFileContent(e.toAbsolutePath().toString(), false);
                Query query = new Query(queryString);
                query.name(e.getFileName().toString());
                String sqlFilePath = e.toAbsolutePath().toString();
                String outputCsvPath = sqlFilePath.substring(0, sqlFilePath.length() - 3) + "csv";
                query.outputPath(Paths.get(outputCsvPath));

                queries.add(query);
              }
            });
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          LOGGER.info("The directory (" + pathArg1.toAbsolutePath().toString() + ") containing (" + queries.size() + ") SQL files was given as input of the diff.");

          if (queries.size() == 0) {

            System.err.println("The argument 1 (" + pathArg1.toAbsolutePath().toString() + " is a directory that contains no sql file to diff.");
            System.exit(1);

          }

        }
      }


      String jdbcUrl2 = cliParser.getString(JDBC_URL_SOURCE_OPTION);
      if (jdbcUrl2 == null) {
        System.err.println("With only one argument, we are performing a diff between two databases and therefore, the second JDBC URL option (" + JDBC_URL_SOURCE_OPTION + ") is mandatory.");
        CliUsage.print(cliCommand);
        System.exit(1);
      }
      String jdbcDriver2 = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);


      System.out.println("Diff started");

      //TODO: Create a timer
      Date startTime = new Date();

      String jdbcUrl1 = null;
      String jdbcDriver1 = null;
      Connection connection1 = new JdbcConnectionBuilder(null, null, jdbcUrl1, jdbcDriver1).build();
      Statement statement1;
      try {
        statement1 = connection1.createStatement();

        statement1.setQueryTimeout(300);

        Connection connection2 = new JdbcConnectionBuilder(null, null, jdbcUrl2, jdbcDriver2).build();
        Statement statement2 = connection2.createStatement();
        statement2.setQueryTimeout(300);

        if (batchOutputPrinter != null) {

          List<Object> csvRow = new ArrayList<>();
          csvRow.add("Query Name");
          csvRow.add("Error");
          csvRow.add("Error Message");
          csvRow.add("Diff");
          batchOutputPrinter.printRecord(csvRow);

        }

        for (Query query : queries) {

          LOGGER.info("A Diff on the query (" + query.getName() + ") was started");
          try (


            ExecuteQueryThread executeQueryThread1 = new ExecuteQueryThread("connection1", statement1, query.getQuery());
            ExecuteQueryThread executeQueryThread2 = new ExecuteQueryThread("connection2", statement2, query.getQuery())

          ) {

            Thread t1 = new Thread(executeQueryThread1);
            Thread t2 = new Thread(executeQueryThread2);

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            ResultSet resultSet1 = null;
            Boolean error = false;
            StringBuilder error_message = new StringBuilder();
            if (!executeQueryThread1.isError()) {
              resultSet1 = executeQueryThread1.getResultSet();
            } else {
              error = true;
              error_message.append("The connection 1 gives the following error:").append(executeQueryThread1.getError().getMessage());
            }

            ResultSet resultSet2 = null;
            if (!executeQueryThread1.isError()) {
              resultSet2 = executeQueryThread2.getResultSet();
            } else {
              error = true;
              error_message.append("The connection 1 gives the following error:").append(executeQueryThread1.getError().getMessage());
            }

            List<Object> csvRow = new ArrayList<>();
            csvRow.add(query.getName());
            csvRow.add(error);
            csvRow.add(error_message.toString());


            if (!error) {

              DataSetDiff dataSetDiff = new DataSetDiff(resultSet1, resultSet2, null, query.getOutputPath());
              Boolean diff = dataSetDiff.diff();
              //TODO: create a feedback object
              LOGGER.info("The Diff on the query (" + query.getName() + ") has ended");
              if (diff) {
                LOGGER.info("There was a diff");
              } else {
                LOGGER.info("No diff found");
              }

              csvRow.add(diff);


            } else {

              csvRow.add(null);

            }

            if (batchOutputPrinter != null) {

              batchOutputPrinter.printRecord(csvRow);
              batchOutputPrinter.flush();

            }

          } catch (Exception e) {
            throw new RuntimeException(e);
          }

        }

        // Close
        statement1.close();
        connection1.close();
        statement2.close();
        connection2.close();


        Date endTime = new Date();
        long totalDiff = endTime.getTime() - startTime.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = 1000 * 60;
        long hoursInMilli = 1000 * 60 * 60;

        long elapsedHours = totalDiff / hoursInMilli;

        long diff = totalDiff % hoursInMilli;
        long elapsedMinutes = diff / minutesInMilli;

        diff = diff % minutesInMilli;
        long elapsedSeconds = diff / secondsInMilli;

        diff = diff % secondsInMilli;
        long elapsedMilliSeconds = diff;

        LOGGER.info("The diff session has ended");
        LOGGER.info(String.format("Response Time for the load of the table  target workers: %d:%d:%d.%d (hour:minutes:seconds:milli)", elapsedHours, elapsedMinutes, elapsedSeconds, elapsedMilliSeconds));
        LOGGER.info(String.format("       Ie (%d) milliseconds%n", totalDiff));

        // Close the batch output
        if (batchOutputPrinter != null) {

          batchOutputPrinter.close();

        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    } else {

      System.err.println("Two arguments is not yet implemented sorry.");

      // CsvCalciteResultSet csvResultSet = new CsvCalciteResultSet(fileSourcePath);
      // Close the csvResultSet
      // csvResultSet.close();

    }


  }

  /**
   * A convenient class to manipulate queries
   */
  private static class Query {

    private final String sqlQuery;
    private Path path; // The SQL file where the query was saved
    private String name;

    Query(String sqlQuery) {
      this.sqlQuery = sqlQuery;
    }

    void outputPath(Path outputPathArg) {
      this.path = outputPathArg;
    }

    public String getQuery() {
      return sqlQuery;
    }

    Path getOutputPath() {
      return path;
    }

    public String getName() {
      return name;
    }

    /**
     * @param queryName - A query name for the output log
     */
    public void name(String queryName) {
      this.name = queryName;
    }

  }
}
