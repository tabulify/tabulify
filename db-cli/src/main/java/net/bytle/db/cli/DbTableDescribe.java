package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


public class DbTableDescribe {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final String ARG_NAME = "(name|pattern)..";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Show the structure of a table";


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

        System.out.println();

        if (tableDefList.size() != 0) {

            switch (tableDefList.size()) {
                case 1:
                    Tables.printColumns(tableDefList.get(0));
                    break;
                default:

                    for (TableDef tableDef : tableDefList) {
                        System.out.println();
                        System.out.println("  * " + tableDef.getName() + " columns:");
                        Tables.printColumns(tableDef);
                    }


            }
        } else {

            System.out.println("No tables found");

        }
        System.out.println();
        LOGGER.info("Bye !");


    }


}
