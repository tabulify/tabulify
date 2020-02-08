package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;

import java.nio.file.Path;

import static net.bytle.db.cli.Words.*;


public class DbTableLoad {


  public static void run(CliCommand cliCommand, String[] args) {

    // Create the command
    cliCommand.setDescription("Load one ore more csv files into a database.");
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a table is not found with the source table Uri")
      .setDefaultValue(false);
    cliCommand.argOf(SOURCE_DATA_URI)
      .setDescription("A source data uri pattern that represents one or more CSV file(s). Example: `*.csv`")
      .setMandatory(true);
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A target data uri that represents the target table (Example: [name]@datastore). The name is optional and will be taken from the sources if not present")
      .setMandatory(true);

    // Args
    final CliParser cliParser = Clis.getParser(cliCommand, args);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final String dataUriPattern = cliParser.getString(SOURCE_DATA_URI);
    final String targetDataUriArg = cliParser.getString(TARGET_DATA_URI);


    DbStatic.transfers(dataUriPattern, targetDataUriArg, storagePathValue, notStrictRun);

  }


}
