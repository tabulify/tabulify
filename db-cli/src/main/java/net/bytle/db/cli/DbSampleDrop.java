package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;

import java.util.List;
import java.util.logging.Level;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


public class DbSampleDrop {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "schemaName";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Drop the tables that belongs to a sample database schema.";


        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("The name of the sample schema. One of " + String.join(", ", DbSamples.getNames()))
                .setMandatory(true);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));

        String sampleName = cliParser.getString(ARG_NAME);
        if (!DbSamples.getNames().contains(sampleName)) {
            System.err.println();
            System.err.println("The sample schema (" + sampleName + ") is unknown");
            System.err.println("It must be one of (" + String.join(", ", DbSamples.getNames()) + ")");
            System.exit(1);
        }

        Dag dag = Dag.get(DbSamples.getTables(sampleName));
        List<TableDef> tables = dag.getDropOrderedTables();

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        for (TableDef tableDef : tables) {
            if (Tables.exists(tableDef, database)) {
                Tables.drop(tableDef, database);
                System.out.println("Table (" + tableDef.getName() + ") was dropped.");
            } else {
                System.out.println("Table (" + tableDef.getName() + ") doesn't exist.");
            }
        }

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        LOGGER.info("Bye !");


    }


}
