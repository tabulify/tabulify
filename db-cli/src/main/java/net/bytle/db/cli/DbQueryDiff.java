package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.engine.Queries;
import net.bytle.db.resultSetDiff.DataSetDiff;
import net.bytle.db.resultSetDiff.DataSetDiffResult;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbQueryDiff {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbQueryDiff.class);
  public static final String FIRST_QUERY_URI = "QueryUri";
  public static final String SECOND_QUERY_URI = "QueryUri";


  public static void run(CliCommand cliCommand, String[] args) {

    // The command
    cliCommand
      .setDescription("Performs a diff between two queries.")
      .addExample(Strings.multiline("Diff between two different queries:", DIFF_COMMAND + " queryFile1.sql@sqlite queryFile2.sql@sqlite"))
      .addExample(Strings.multiline("Diff between two different datastore:", DIFF_COMMAND + " queryFile.sql@sqlite queryFile.sql@postgres"));
    cliCommand.argOf(FIRST_QUERY_URI).setDescription("A query URI pattern that define the first query(ies) to compare (Example: dim*.sql@datastore)")
      .setMandatory(true);
    cliCommand.argOf(SECOND_QUERY_URI).setDescription("A query URI pattern that define the second query(ies) to compare (Example: dim*.sql@datastore)")
      .setMandatory(true);
    cliCommand.optionOf(OUTPUT_DATA_URI)
      .setDescription("defines the destination of the diff as a data uri");
    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final String firstQueryArg = cliParser.getString(FIRST_QUERY_URI);
    final String secondQueryArg = cliParser.getString(SECOND_QUERY_URI);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);

    // Main
    DataUri firstQueryUri = DataUri.of(firstQueryArg);
    DataUri secondQueryUri = DataUri.of(secondQueryArg);

    // Files selected
    List<Path> firstQueryUriFiles = Fs.getFilesByGlob(firstQueryUri.getPath());
    if (firstQueryUriFiles.size() == 0) {
      LOGGER.error("There was no sql file selected with the first query uri pattern ({})", firstQueryArg);
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
        tabular.withDefaultDataStoreVault();
      }

      // A structure to get the feedback
      DataPath feedback = tabular.getDataPath("feedback")
        .getDataDef()
        .addColumn("Query Diff Name")
        .addColumn("Result")
        .addColumn("Error")
        .addColumn("Error Message")
        .getDataPath();
      Tabulars.create(feedback);

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
        String diffName = queryDiffNameBuilder.toString();

        // Feedback
        LOGGER.info("Diff between the first query against the data store `" + firstQueryUri.getDataStore() + "`(from the file " + firstFile.normalize().toAbsolutePath().toString() + ")");
        LOGGER.info("   * " + Strings.normalize(firstQuery));
        LOGGER.info("and the second query against the data store `" + firstQueryUri.getDataStore() + "` (from the file " + secondFile.normalize().toAbsolutePath().toString() + ")");
        LOGGER.info("   * " + Strings.normalize(secondQuery));

        // Diff between two different databases
        Path outputPathArg = cliParser.getPath(OUTPUT_DATA_URI);

        System.out.println("Diff started");


        // TODO: statement1.setQueryTimeout(300);

        LOGGER.info("A Diff on the query (" + diffName + ") was started");
        Timer cliTimer = Timer.getTimer(diffName).start();

        DataPath firstQueryDataPath = tabular.getDataStore(firstQueryUri.getDataStore())
          .getQueryDataPath(firstQuery);
        DataPath secondQueryDataPath = tabular.getDataStore(secondQueryUri.getDataStore())
          .getQueryDataPath(secondQuery);

        // Diff
        LOGGER.info("The Diff (" + diffName + ") has started");
        DataSetDiff dataSetDiff = new DataSetDiff(firstQueryDataPath, secondQueryDataPath);
        if (outputPathArg!=null){
          dataSetDiff.setResultDataPath(tabular.getDataPath(outputPathArg));
        }
        DataSetDiffResult result = dataSetDiff.diff();
        LOGGER.info("The Diff (" + diffName + ") has ended");
        if (result.areEquals()) {
          LOGGER.info("The result of the queries are NOT equals");
        } else {
          LOGGER.info("The result of the queries are equals");
        }

        cliTimer.stop();

        LOGGER.info(String.format("Response Time for the comparison: %s (hour:minutes:seconds:milli), %d (milliseconds)%n", cliTimer.getResponseTime(), cliTimer.getResponseTimeInMilliSeconds()));

      }

    }
  }

}



