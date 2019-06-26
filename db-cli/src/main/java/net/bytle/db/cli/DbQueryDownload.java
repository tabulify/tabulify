package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Queries;
import net.bytle.db.engine.Strings;
import net.bytle.db.model.QueryDef;
import net.bytle.log.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbQueryDownload {


    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;

    private static final String ARG_NAME = "(Query|File.sql)";
    private static final String CLOB_OPTION = "clob";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(ARG_NAME);
        cliCommand.optionOf(Words.OUTPUT_FILE_PATH)
                .setDescription("defines the path of the output path (Ex: output/query.csv)");
        cliCommand.flagOf(CLOB_OPTION)
                .setShortName("c")
                .setDescription("If present, the values of clob columns will be written in a apart file");
        String footer = "\nExample:\n" +
                cliCommand.getName() + CliParser.PREFIX_LONG_OPTION + Words.OUTPUT_FILE_PATH + " QueryDownloaded.csv QueryToDownload.sql \n";
        cliCommand.setFooter(footer);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);
        cliCommand.optionOf(CLOB_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        String sourceURL = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String sourceDriver = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);

        Boolean clobInApartFile = cliParser.getBoolean(CLOB_OPTION);

        Path pathDownloadFile = null;
        String downloadPathArg = cliParser.getString(OUTPUT_FILE_PATH);
        if (downloadPathArg != null) {
            pathDownloadFile = Paths.get(downloadPathArg);
        }


        List<String> argValues = cliParser.getStrings(ARG_NAME);
        if (argValues.size() != 1) {
            System.err.println("An argument must be given");
            CliUsage.print(cliCommand);
            System.exit(1);
        }

        String arg0 = argValues.get(0);
        String sourceFileQuery;
        boolean isRegularFile = Files.isRegularFile(Paths.get(arg0));
        if (isRegularFile) {
            sourceFileQuery = cliParser.getFileContent(ARG_NAME, false);
        } else {
            sourceFileQuery = arg0;
        }

        // Problem we have no query
        if (!Queries.isQuery(sourceFileQuery)) {

            System.err.println("The first argument is expected to be an SQL file or a SQL query containing the SELECT keyword.");
            if (isRegularFile) {
                System.err.println("The first argument (" + arg0 + ") is a file but its content below does not contain the SELECT word in the first positions");
                System.err.println("Query: \n" + Strings.toStringNullSafe(sourceFileQuery));
            } else {
                System.err.println("The first argument is not a file and its value below does not contain the SELECT word in the first positions");
                System.err.println("Arg Value: \n" + Strings.toStringNullSafe(arg0));
            }
            CliUsage.print(cliParser.getCommand());
            System.exit(1);

        }


        CliTimer cliTimer = CliTimer.getTimer("download").start();

        System.out.println("Download process started");
        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(sourceURL)
                .setDriver(sourceDriver);
        Connection connection = database.getCurrentConnection();
        LOGGER.info("Connection successful - Querying and Downloading the data");

        QueryDef queryDef = database.getQuery(sourceFileQuery);
        int exitStatus = Queries.download(queryDef, pathDownloadFile, clobInApartFile);

        // Feedback
        cliTimer.stop();

        System.out.printf("Response Time to downloader the data: %s (hour:minutes:seconds:milli)%n", cliTimer.getResponseTime());
        System.out.printf("       Ie (%d) milliseconds%n", cliTimer.getResponseTimeInMilliSeconds());

        // Exit
        if (exitStatus != 0) {
            System.out.println("Error ! (" + exitStatus + ") errors were seen.");
        } else {
            System.out.println("Success ! No errors were seen.");
        }
        System.exit(exitStatus);

    }


}
