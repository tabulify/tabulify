package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;

import java.sql.Types;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbTableList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "(name|pattern)..";



    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Print a list of tables.";


        // Create the parser
        cliCommand
                .setDescription(description);
        cliCommand.argOf(ARG_NAME)
                .setDescription("One or more name of a table (or glob expression)")
                .setMandatory(false)
                .setDefaultValue("*");

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        cliCommand.flagOf(Words.COUNT_COMMAND)
                .setDescription("suppress the count column")
                .setShortName("c");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));

        final Boolean withCount = !(cliParser.getBoolean(Words.COUNT_COMMAND));

        List<String> patterns = cliParser.getStrings(ARG_NAME);
        List<TableDef> tableDefList = database.getCurrentSchema().getTables(patterns);
        System.out.println();
        if (tableDefList.size() != 0) {

            TableDef printTable = Tables.get("tables")
                    .addColumn("Table Name");

            if (withCount) {
                printTable.addColumn("Count", Types.INTEGER);
            }

            InsertStream insertStream = Tables.getTableInsertStream(printTable);
            for (TableDef tableDef : tableDefList) {

                if (withCount) {
                    Integer count = Tables.getSize(tableDef);
                    insertStream.insert(tableDef.getName(), count);
                } else {
                    insertStream.insert(tableDef.getName());
                }
            }
            insertStream.close();


            System.out.println("Tables:");
            System.out.println();
            Tables.print(printTable);
            Tables.drop(printTable);

        } else {

            System.out.println("No tables found");

        }
        System.out.println();

        LOGGER.info("Bye !");


    }


}
