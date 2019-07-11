package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.Log;
import net.bytle.db.database.Databases;


/**
 * <p>
 */
public class DbDatabaseList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;



    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List the databases";

        // Create the parser
        cliCommand
                .setDescription(description);

        Databases.of();

        LOGGER.info("Bye !");


    }


}
