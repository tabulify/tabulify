package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.DbDatabase.BYTLE_DB_DATABASES_PATH;
import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;


/**
 * <p>
 */
public class DbDatabaseRemove {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATABASE_PATTERN = "name";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List the databases";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(DATABASE_PATTERN)
                .setDescription("the database name or a glob pattern")
                .setMandatory(true);

        cliCommand.optionOf(DbDatabase.STORAGE_PATH)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatabase.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        final List<String> names = cliParser.getStrings(DATABASE_PATTERN);

        LOGGER.info("Bye !");


    }


}
