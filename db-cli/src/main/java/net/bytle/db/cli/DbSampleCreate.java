package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;
import net.bytle.db.sample.SchemaSample;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


public class DbSampleCreate {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final String ARG_NAME = "schemaName";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Create sample database schema.";


        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("The name of the sample schema. One of " + String.join(", ", DbSamples.getNames()))
                .setMandatory(true);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));

        String sampleName = cliParser.getString(ARG_NAME);
        if (!DbSamples.getNames().contains(sampleName)) {
            System.err.println();
            System.err.println("The sample schema (" + sampleName + ") is unknown");
            System.err.println("It must be one of (" + String.join(", ", DbSamples.getNames()) + ")");
            System.exit(1);
        }

        SchemaSample schemaSample = DbSamples.get(sampleName);
        Dag dag = Dag.get(schemaSample.getTables());
        List<TableDef> tables = dag.getCreateOrderedTables();

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        for (TableDef tableDef : tables) {
            if (!Tables.exists(tableDef, database)) {
                Tables.create(tableDef, database);
                System.out.println("Table (" + tableDef.getName() + ") created.");
            } else {
                System.out.println("Table (" + tableDef.getName() + ") already exist.");
            }
        }

        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        LOGGER.info("Bye !");


    }


}
