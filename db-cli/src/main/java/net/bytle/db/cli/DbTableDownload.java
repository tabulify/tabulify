package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static net.bytle.db.cli.Words.*;

/**
 * To download data on the local file system
 *
 * TODO: same download code than {@link DbQueryDownload} {@link DbTableLoad}  - Do we merge the code ?
 */
public class DbTableDownload {


  private static final Logger LOGGER = LoggerFactory.getLogger(DbTableDownload.class);
  ;
  private static final String CLOB_OPTION = "cif";


  public static void run(CliCommand cliCommand, String[] args) {

    cliCommand.setDescription("Download one or more table(s) into one or more csv file(s).");
    cliCommand.argOf(SOURCE_DATA_URI)
      .setDescription("A data URI pattern that defines the tables to download")
      .setMandatory(true);
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A data URI that defines the location of the downloaded file (Example: data.csv@file or dir/@file). If the target is a directory, the name of the files will be the name of the tables.")
      .setDefaultValue(".")
      .setMandatory(true);
    cliCommand.addExample(Strings.multiline(
      "To download the table `time` from the data store `sqlite` into the file `time.csv`, you would execute",
      cliCommand.getName() + " time@sqlite time.csv"
    ));
    cliCommand.addExample(Strings.multiline(
      "To download all the table that starts with `dim` from the data store `oracle` into the directory `dim`, you would execute",
      cliCommand.getName() + " dim*@oracle dim/",
      "In non strict mode, if the directory does not exist or if a file exists, they will be replaced"
    ));
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will replace the files existing and not throw an errors for minor problem (example if a table was not found with a pattern) ")
      .setDefaultValue(false);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String sourceDataUriPatternArg = cliParser.getString(SOURCE_DATA_URI);
    final String targetDataUriArg = cliParser.getString(TARGET_DATA_URI);

    // Main
    Dbs.transfers(sourceDataUriPatternArg, targetDataUriArg, storagePathValue, notStrictRunArg);

  }
}
