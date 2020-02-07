package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;

/**
 *
 * To download the data of a query
 */
public class DbQueryDownload {


  private static final Logger LOGGER = LoggerFactory.getLogger(DbQueryDownload.class);

  private static final String SOURCE_QUERY_URI_PATTERN = "SourceQueryUriPattern";
  private static final String TARGET_DATA_URI = "targetUri";


  public static void run(CliCommand cliCommand, String[] args) {

    // Cli Command
    cliCommand.addExample(Strings.multiline(
      "To download the data of the query defined in the file `QueryToDownload.sql` and executed against the data store `sqlite` into the file `QueryData.csv`, you would execute the following command:",
      cliCommand.getName() + " QueryToDownload.sql@sqlite QueryData.csv"
    ));
    cliCommand.addExample(Strings.multiline(
      "To download the data of all query defined in all `sql` files of the current directory, execute them against the data store `sqlite` and save the results into the directory `result`, you would execute the following command:",
      cliCommand.getName() + " *.sql@sqlite result"
    ));
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.argOf(SOURCE_QUERY_URI_PATTERN)
      .setDescription("The source query URI pattern");
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A data URI that defines the destination (a file or a directory)");
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will replace the files existing and not throw an errors for minor problem (example if a table was not found with a pattern) ")
      .setDefaultValue(false);

    // Parse and args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String sourceQueryUriArg = cliParser.getString(SOURCE_QUERY_URI_PATTERN);
    final String targetDataUriArg = cliParser.getString(TARGET_DATA_URI);

    // Main
    Dbs.transfers(sourceQueryUriArg, targetDataUriArg, storagePathValue, notStrictRunArg);

  }


}
