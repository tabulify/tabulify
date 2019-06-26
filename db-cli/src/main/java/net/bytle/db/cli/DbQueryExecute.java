package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Fs;
import net.bytle.db.engine.Queries;
import net.bytle.db.engine.Strings;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.MemoryInsertStream;
import net.bytle.db.stream.SelectStreamListener;
import net.bytle.log.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbQueryExecute {


    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;

    private static final String ARG_NAME = "Query|File.sql|Directory";
    private static final int ARG_TYPE = 0;
    private static final int FILE_TYPE = 1;
    private static final int DIRECTORY_TYPE = 2;


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(ARG_NAME)
                .setDescription("The query defines as a command line argument, a query file or a directory of query files");

        cliCommand.setDescription("Execute one or several queries");


        String footer = "\nExample:\n" +
                Words.CLI_NAME + " " + cliCommand.getName() + " " + CliParser.PREFIX_LONG_OPTION + Words.OUTPUT_FILE_PATH + " QueryDownloaded.csv QueryToDownload.sql \n";

        cliCommand.setFooter(footer);

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<String> argValues = cliParser.getStrings(ARG_NAME);
        if (argValues.size() != 1) {
            System.err.println("An argument must be given");
            CliUsage.print(cliCommand);
            System.exit(1);
        }

        // Database
        String sourceURL = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String sourceDriver = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);
        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(sourceURL)
                .setDriver(sourceDriver);


        // Arg Type
        Integer argType;
        String arg0 = argValues.get(0);
        if (Fs.isFile(arg0)) {
            argType = FILE_TYPE;
        } else if (Fs.isDirectory(arg0)) {
            argType = DIRECTORY_TYPE;
        } else {
            argType = ARG_TYPE;
        }

        // Check
        List<QueryDef> queries = new ArrayList<>();
        String query;
        switch (argType) {
            case FILE_TYPE:
                query = cliParser.getFileContent(ARG_NAME, false);
                if (Queries.isQuery(query)) {
                    queries.add(database.getQuery(query));
                } else {
                    System.err.println("The first argument is a file (" + arg0 + ") that seems to not contain a query");
                    System.err.println("The file content is: ");
                    System.err.println(Strings.toStringNullSafe(query));
                    CliUsage.print(cliCommand);
                    System.exit(1);
                }
                break;
            case DIRECTORY_TYPE:
                try {
                    LOGGER.info("Scanning the directory (" + arg0 + ")");
                    for (Path path : Files.newDirectoryStream(Paths.get(arg0))) {
                        // Not sub-directory yet
                        if (Files.isRegularFile(path) && path.toString().endsWith("sql")) {
                            query = Fs.getFileContent(path);
                            if (Queries.isQuery(query)) {
                                QueryDef fileQueryDef = database.getQuery(query)
                                        .setName(path.getFileName().toString());
                                queries.add(fileQueryDef);
                            } else {
                                System.err.println("The execution of the directory (" + arg0 + ") was asked but");
                                System.err.println("the content of the file (" + path + ") is not a query");
                                System.err.println("The content is: ");
                                System.err.println(Strings.toStringNullSafe(query));
                                CliUsage.print(cliCommand);
                                System.exit(1);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case ARG_TYPE:
                if (Queries.isQuery(arg0)) {
                    queries.add(database.getQuery(arg0));
                } else {
                    System.err.println("The first argument value seems not to be a file, a directory or a query");
                    System.err.println("The argument value is: ");
                    System.err.println(Strings.toStringNullSafe(arg0));
                    CliUsage.print(cliCommand);
                    System.exit(1);
                }
                break;
            default:
                throw new RuntimeException("not possible");
        }


        LOGGER.info("Processing the request");
        switch (queries.size()) {
            case 0:
                System.out.println();
                System.out.println("No query found");
                break;

            case 1:

                // Prep
                CliTimer cliTimer = CliTimer.getTimer("execute").start();

                // Begin output
                DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
                System.out.println();
                Queries.print(queries.get(0));
                System.out.println();
                DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);

                // Feedback
                cliTimer.stop();
                LOGGER.info("Response Time to query the data: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
                LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");
                break;

            default:

                TableDef executionTable = Tables.get("executions");
                executionTable
                        .addColumn("Query Name", Types.VARCHAR)
                        .addColumn("Latency (ms)", Types.INTEGER)
                        .addColumn("Row Count", Types.INTEGER)
                        .addColumn("Error", Types.VARCHAR)
                        .addColumn("Message", Types.VARCHAR);
                InsertStream exeInput = MemoryInsertStream.get(executionTable);

                for (QueryDef queryDef : queries) {

                    cliTimer = CliTimer.getTimer("execute").start();
                    String rowCount = "";
                    String status = "";
                    String message = "";
                    try {
                        SelectStreamListener feedback = Queries.execute(queryDef);
                        rowCount = String.valueOf(feedback.getRowCount());
                    } catch (Exception e) {
                        status = "Err";
                        message = Log.onOneLine(e.getMessage());
                        LOGGER.severe(e.getMessage());
                    }

                    cliTimer.stop();
                    exeInput.insert(queryDef.getName(), cliTimer.getResponseTimeInMilliSeconds(), rowCount, status, message);

                }
                exeInput.close();
                System.out.println();
                Tables.print(executionTable);
                System.out.println();

                break;
        }

        LOGGER.info("Bye !");

    }

}
