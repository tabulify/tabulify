package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliTimer;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;
import net.bytle.db.gen.DataGenLoader;
import net.bytle.db.gen.yml.DataGenYml;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableFill {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;
    private static final int TABLE_TYPE = 1;
    private static final Integer YML_TYPE = 2;

    static public final String NUMBER_OF_ROWS_OPTION = "rows";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Load generated data into a table";


        // Create the parser
        final String ARG_NAME = "(TABLE|DataGen.yml)";
        cliCommand
                .setDescription(description);
        String desc = "A table name or a data definition file (DataGen.yml)";

        cliCommand.argOf(ARG_NAME)
                .setDescription(desc)
                .setMandatory(true);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        cliCommand.optionOf(NUMBER_OF_ROWS_OPTION)
                .setDescription("defines the total number of rows that the table must have")
                .setDefaultValue(100);

        CliParser cliParser = Clis.getParser(cliCommand, args);


        // ARg
        String argument = cliParser.getString(ARG_NAME);
        Integer typeArgument = TABLE_TYPE;
        int lastPointIndex = argument.lastIndexOf(".");
        if (lastPointIndex != -1) {
            String extension = argument.substring(lastPointIndex + 1, argument.length());
            if (extension.equals("yml")) {
                typeArgument = YML_TYPE;
            }
        }
        if (typeArgument == TABLE_TYPE) {
            LOGGER.info("Loading generated data for the table " + argument);
        } else {
            LOGGER.info("Loading generated data with the data definition file (" + argument + ")");
        }

        CliTimer cliTimer = CliTimer.getTimer(argument).start();

        // Database
        String url = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String driver = cliParser.getString(JDBC_DRIVER_TARGET_OPTION);
        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(url)
                .setDriver(driver);

        if (typeArgument == YML_TYPE) {
            Path path = cliParser.getPath(ARG_NAME);
            InputStream input;
            try {
                input = Files.newInputStream(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DataGenYml dataGenYml = new DataGenYml(database, input).loadParentTable(true);
            List<TableDef> tables = dataGenYml.load();

            LOGGER.info("The following tables where loaded:");
            for (TableDef tableDef : tables) {
                LOGGER.info("  * " + tableDef.getFullyQualifiedName() + ", Size (" + Tables.getSize(tableDef) + ")");
            }

        } else {
            TableDef tableDef = database.getTable(argument);
            if (!Tables.exists(tableDef)) {
                LOGGER.severe("The table (" + tableDef.getFullyQualifiedName() + " doesn't exist.");
                System.exit(1);
            } else {
                LOGGER.info("The table (" + tableDef.getFullyQualifiedName() + ") has (" + Tables.getSize(tableDef) + ") rows before loading.");
            }

            Integer totalNumberOfRows = cliParser.getInteger(NUMBER_OF_ROWS_OPTION);
            DataGenLoader.get(tableDef)
                    .setRows(totalNumberOfRows)
                    .load();

            LOGGER.info("The table (" + tableDef.getFullyQualifiedName() + ") has now (" + Tables.getSize(tableDef) + ") rows");
        }

        cliTimer.stop();

        LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)%n");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


        LOGGER.info("Success ! No errors were seen.");

    }


}
