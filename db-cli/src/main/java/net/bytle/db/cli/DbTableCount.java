package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;

public class DbTableCount {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final String ARG_NAME = "pattern..";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Count the number of tables in the current schema";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("one or more regular expressions.")
                .setMandatory(false)
                .setDefaultValue(".*");
        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));


        List<String> patterns = cliParser.getStrings(ARG_NAME);
        Integer size = 0;
        for (String pattern : patterns) {
            size += database.getCurrentSchema().getTables(pattern).size();
        }
        System.out.println(size + " tables");

        LOGGER.info("Bye !");

    }


}

