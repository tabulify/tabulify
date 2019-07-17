package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Queries;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbTableDownload {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "TableName";
    private static final String CLOB_OPTION = "cif";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.setDescription("Download a table into a file.");
        cliCommand.argOf(ARG_NAME)
                .setMandatory(true);
        cliCommand.optionOf(OUTPUT_FILE_PATH)
                .setDescription("defines the directory of the downloaded file (Default: currentWorkingDirectory)");
        cliCommand.optionOf(CLOB_OPTION)
                .setDescription("If present, the values of clob columns will be written in a apart file");
        String footer = "\nExample:\n" +
                cliCommand.getName() + "-tdf downloaded.csv toDownload.sql \n";
        cliCommand.setFooter(footer);

        cliCommand.optionOf(CLOB_OPTION);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        String sourceURL = "";
        String sourceDriver = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);
        Path downloadPathDir = cliParser.getPath(OUTPUT_FILE_PATH);
        if (downloadPathDir == null) {
            downloadPathDir = Paths.get(".");
        }
        Boolean clobInApartFile = cliParser.getBoolean(CLOB_OPTION);


        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(sourceURL)
                .setDriver(sourceDriver);
        Connection connection = database.getCurrentConnection();

        List<CliTimer> timers = new ArrayList<>();
        int errors = 0;
        List<String> argValues = cliParser.getStrings(ARG_NAME);
        for (String tableName : argValues) {

            TableDef tableDef = database.getTable(tableName);
            if (!Tables.exists(tableDef)) {
                LOGGER.severe("The table (" + tableDef.getFullyQualifiedName() + ") does not exist.");
                errors++;
            }
            Path pathDownloadFile = Paths.get(downloadPathDir.toString(), tableName + ".csv");
            String sourceQuery = "select * from " + tableDef.getFullyQualifiedName();

            LOGGER.info("Download process started for the table " + tableDef.getFullyQualifiedName());
            CliTimer cliTimer = CliTimer.getTimer(tableName).start();
            QueryDef queryDef = database.getQuery(sourceQuery);
            int exitStatus = Queries.download(queryDef, pathDownloadFile, clobInApartFile);
            LOGGER.info("The file can be found at: " + pathDownloadFile.toAbsolutePath().normalize());
            cliTimer.stop();
            timers.add(cliTimer);

            if (exitStatus != 0) {
                errors++;
            }

        }

        // Feedback
        for (CliTimer timer : timers) {
            LOGGER.info("Response Time to download the table " + timer.getName() + " : " + timer.getResponseTime() + " (hour:minutes:seconds.milli) Ie " + timer.getResponseTimeInMilliSeconds() + " milliseconds.");
        }

        // Exit
        if (errors != 0) {
            LOGGER.severe("Error ! (" + errors + ") errors were seen.");
            // Exit only if there is an error
            System.exit(errors);
        } else {
            LOGGER.info("Success ! No errors were seen.");
        }

    }


}
