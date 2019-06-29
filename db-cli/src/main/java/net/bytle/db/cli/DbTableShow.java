package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Queries;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.TableDef;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;

public class DbTableShow {

    public static final Log LOGGER_DB_ENGINE = DbLoggers.LOGGER_DB_ENGINE;
    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "(name|pattern)..";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Show the data of a table";


        // Create the parser
        cliCommand
                .setDescription(description);
        cliCommand.argOf(ARG_NAME)
                .setDescription("The name of a table (or regular expression)")
                .setMandatory(true);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));

        List<String> patterns = cliParser.getStrings(ARG_NAME);
        List<TableDef> tableDefList = database.getCurrentSchema().getTables(patterns);
        LOGGER_DB_ENGINE.info("db table show called: Starting to query the tables");

        System.out.println();
        if (tableDefList.size() != 0) {

            // Prep
            CliTimer cliTimer = CliTimer.getTimer("execute").start();
            LOGGER_DB_ENGINE.setLevel(Level.WARNING);
            System.out.println();

            switch (tableDefList.size()) {
                case 1:
                    queryPrint(tableDefList.get(0));
                    System.out.println();
                    break;

                default:

                    for (TableDef tableDef : tableDefList) {

                        System.out.println("Data from the table (" + tableDef.getName() + "): ");
                        queryPrint(tableDef);
                        System.out.println();

                    }
                    break;

            }

            // Feedback
            cliTimer.stop();
            DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
            LOGGER.info("Response Time to query the data: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
            LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");


        } else {

            System.out.println("No tables found. Nothing to show.");

        }
        System.out.println();
        LOGGER.info("Bye !");


    }

    protected static void queryPrint(TableDef tableDef) {
        Database database = tableDef.getDatabase();
        // TODO: Add a limit clause !
        QueryDef query = database.getQuery("select * from " + tableDef.getFullyQualifiedName());
        Queries.print(query);
    }


}
