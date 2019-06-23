package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.db.DbLoggers;

import java.util.logging.Logger;


/**
 * <p>
 */
public class DbDatabaseList {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final String DATABASE_NAME = "name";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List databases";

        String footer = "";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setFooter(footer);

        // To continue

        LOGGER.info("Bye !");


    }


}
