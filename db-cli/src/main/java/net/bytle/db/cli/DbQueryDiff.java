package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliWord;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.engine.Queries;
import net.bytle.db.resultSetDiff.DataSetDiff;
import net.bytle.db.resultSetDiff.ExecuteQueryThread;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbQueryDiff {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbQueryDiff.class);


  public static void run(CliCommand cliCommand, String[] args) {

    // The command
    cliCommand
      .setDescription("Performs a diff between two queries.")
      .addExample(Strings.multiline("Diff between two different queries:", DIFF_COMMAND + " queryFile1.sql@sqlite queryFile2.sql@sqlite"))
      .addExample(Strings.multiline("Diff between two different datastore:", DIFF_COMMAND + " queryFile.sql@sqlite queryFile.sql@postgres"));
    CliWord firstQueryWord = cliCommand.argOf("QueryUri").setDescription("A query URI pattern (Example: dim*.sql@datastore)")
      .setMandatory(true);
    CliWord secondQueryWord = cliCommand.argOf("QueryUri").setDescription("A query URI pattern (Example: dim*.sql@datastore)")
      .setMandatory(true);
    cliCommand.optionOf(OUTPUT_DATA_URI)
      .setDescription("defines the destination of the diff as a data uri");
    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final String firstQueryArg = cliParser.getString(firstQueryWord);
    final String secondQueryArg = cliParser.getString(secondQueryWord);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);

    // Main
    DataUri firstQueryUri = DataUri.of(firstQueryArg);
    DataUri secondQueryUri = DataUri.of(secondQueryArg);

    // Files selected
    List<Path> firstQueryUriFiles = Fs.getFilesByGlob(firstQueryUri.getPath());
    if (firstQueryUriFiles.size() == 0) {
      LOGGER.error("There was not file selected for the first query uri ({})", firstQueryArg);
    }
    List<Path> secondQueryUriFiles = Fs.getFilesByGlob(secondQueryUri.getPath());
    if (secondQueryUriFiles.size() == 0) {
      LOGGER.error("There was not file selected for the second query uri ({})", secondQueryArg);
    }
    if (firstQueryUriFiles.size() != secondQueryUriFiles.size()) {
      LOGGER.error("The number of files selected between the first query uri ({}) and the second are not the same ({})", firstQueryUriFiles.size(), secondQueryUriFiles.size());
      LOGGER.error("The first Query URI ({}) has selected this files {}", firstQueryArg, firstQueryUriFiles);
      LOGGER.error("The second Query URI ({}) has selected this files {}", secondQueryArg, secondQueryUriFiles);
      System.exit(1);
    }

    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      // A structure to get the feedback
      Tabular.tabular().getDataPath("feedback")
        .getDataDef()
        .addColumn("Query Diff Name")
        .addColumn("Error")
        .addColumn("Error Message")
        .addColumn("Diff");

      // Loop through the files
      for (int i = 0; i < firstQueryUriFiles.size(); i++) {
        Path firstFile = firstQueryUriFiles.get(i);
        Path secondFile = secondQueryUriFiles.get(i);
        String firstQuery = Queries.getQuery(firstFile);
        if (firstQuery == null) {
          LOGGER.error("The file (" + firstFile.toAbsolutePath().toString() + ") does not contains any query");
          System.exit(1);
        }
        String secondQuery = Queries.getQuery(secondFile);
        if (secondQuery == null) {
          LOGGER.error("The file (" + secondFile.toAbsolutePath().toString() + ") does not contains any query");
          System.exit(1);
        }

        // Diff name
        StringBuilder queryDiffNameBuilder = new StringBuilder();
        queryDiffNameBuilder.append("query diff " + i);
        if (firstFile.equals(secondFile)) {
          queryDiffNameBuilder.append(" for the file ");
          queryDiffNameBuilder.append(firstFile.getFileName().toString());
          if (firstQueryUri.getDataStore().equals(secondQueryUri.getDataStore())) {
            LOGGER.error("The file ({}) and the datastore ({}) are the same between the two query uri, nothing to compare", firstFile.normalize().toAbsolutePath().toString(), firstQueryUri.getDataStore());
            System.exit(1);
          }
        } else {
          String firstFileDesc = firstFile.getFileName().toString();
          String secondFileDesc = secondFile.getFileName().toString();
          if (firstFileDesc.equals(secondFileDesc)) {
            firstFileDesc = firstFile.normalize().toString();
            secondFileDesc = firstFile.normalize().toString();
          }
          queryDiffNameBuilder.append(" between the files");
          queryDiffNameBuilder.append(firstFileDesc);
          queryDiffNameBuilder.append(" and ");
          queryDiffNameBuilder.append(secondFileDesc);
        }
        String queryDiffName = queryDiffNameBuilder.toString();

        // Feedback
        LOGGER.info("Diff between the first query against the data store `" + firstQueryUri.getDataStore() + "`(from the file " + firstFile.normalize().toAbsolutePath().toString() + ")");
        LOGGER.info("   * " + Strings.normalize(firstQuery));
        LOGGER.info("and the second query against the data store `" + firstQueryUri.getDataStore() + "` (from the file " + secondFile.normalize().toAbsolutePath().toString() + ")");
        LOGGER.info("   * " + Strings.normalize(secondQuery));

        // Diff between two different databases
        Path outputPathArg = cliParser.getPath(OUTPUT_DATA_URI);

        System.out.println("Diff started");


        // TODO: statement1.setQueryTimeout(300);

        LOGGER.info("A Diff on the query (" + queryDiffName + ") was started");
        Timer cliTimer = Timer.getTimer(queryDiffName).start();

        DataPath firstQueryDataPath = tabular.getDataStore(firstQueryUri.getDataStore())
          .getQueryDataPath(firstQuery);
        DataPath secondQueryDataPath = tabular.getDataStore(secondQueryUri.getDataStore())
          .getQueryDataPath(secondQuery);


        ExecuteQueryThread executeQueryThread1 = new ExecuteQueryThread(firstQueryDataPath);
        ExecuteQueryThread executeQueryThread2 = new ExecuteQueryThread(secondQueryDataPath);


        Thread t1 = new Thread(executeQueryThread1);
        Thread t2 = new Thread(executeQueryThread2);
        t1.start();
        t2.start();
        try {
          t1.join();
        } catch (InterruptedException e) {
          LOGGER.error("An exception has occurred during the execution of the first query", e);
          System.exit(1);
        }
        try {
          t2.join();

        } catch (InterruptedException e) {
          LOGGER.error("An exception has occurred during the execution of the second query", e);
          System.exit(1);
        }

        // Check for any errors in the thread

        ResultSet resultSet1 = null;
        Boolean error = false;
        StringBuilder error_message = new StringBuilder();
        if (!executeQueryThread1.isError()) {
          resultSet1 = executeQueryThread1.getSelectStream();
        } else {
          error = true;
          error_message.append("The connection 1 gives the following error:").append(executeQueryThread1.getError().getMessage());
        }

        ResultSet resultSet2 = null;
        if (!executeQueryThread1.isError()) {
          resultSet2 = executeQueryThread2.getSelectStream();
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
      } catch(SQLException e){
        throw new RuntimeException(e);
      } catch(IOException e){
        throw new RuntimeException(e);
      }

    }
  }

}



