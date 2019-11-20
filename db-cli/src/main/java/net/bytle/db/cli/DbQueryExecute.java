package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.engine.Queries;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.log.Log;
import net.bytle.type.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static net.bytle.db.cli.Words.DATABASE_STORE;


public class DbQueryExecute {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String ARG_NAME = "(Query|File.sql|Directory)";
    private static final String DATA_URI = "DataUri";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(DATA_URI)
                .setDescription("A data Uri that define where the execution will take place.")
                .setMandatory(true);
        cliCommand.argOf(ARG_NAME)
                .setDescription("The query defines as a command line argument, a query file or a directory of query files.")
                .setMandatory(true);

        cliCommand.optionOf(DATABASE_STORE);

        cliCommand.setDescription("Execute one or several queries. \n" + "" +
                "For one query, the data is shown. For multiple queries, the performance result is shown.");

        String footer = "\nExample:\n" +
                Words.CLI_NAME + " " + cliCommand.getName() + " QueryToExecute.sql QueryToExecute2.sql\n" +
                Words.CLI_NAME + " " + cliCommand.getName() + " \"select year, count(1) from sales group by year\"\n" +
                Words.CLI_NAME + " " + cliCommand.getName() + " ./directory/withQueries1 QueryToExecute.sql\n";

        cliCommand.setFooter(footer);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        DataUri dataUri = DataUri.of(cliParser.getString(DATA_URI));
        DataPath dataPath = DataPaths.of(databasesStore, dataUri);

        List<String> argValues = cliParser.getStrings(ARG_NAME);


        // Arguments checks
        // Collecting the queries
        Map<String, DataPath> queries = new HashMap<>();
        for (int i = 0; i < argValues.size(); i++) {
            String s = argValues.get(i);
            if (Fs.isFile(s)) {
                final Path path = Paths.get(s);
                String query = Fs.getFileContent(path);
                if (Queries.isQuery(query)) {
                    String queryName = path.getFileName().toString();
                    DataPath dataQueryPath = DataPaths.ofQuery(dataPath, query);
                    queries.put(queryName, dataQueryPath);
                } else {
                    System.err.println("The first argument is a file (" + s + ") that seems to not contain a query");
                    System.err.println("The file content is: ");
                    System.err.println(Strings.toStringNullSafe(query));
                    CliUsage.print(cliCommand);
                    System.exit(1);
                }
            } else if (Fs.isDirectory(s)) {
                try {
                    LOGGER.info("Scanning the directory (" + s + ")");
                    for (Path path : Files.newDirectoryStream(Paths.get(s))) {
                        // Not sub-directory yet
                        if (Files.isRegularFile(path) && path.toString().endsWith("sql")) {
                            String query = Fs.getFileContent(path);
                            if (Queries.isQuery(query)) {
                                String queryName = path.getFileName().toString();
                                DataPath dataQueryPath = DataPaths.ofQuery(dataPath, query);
                                queries.put(queryName, dataQueryPath);
                            } else {
                                System.err.println("The execution of the directory (" + s + ") was asked but");
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
            } else if (Queries.isQuery(s)) {
                DataPath dataQueryPath = DataPaths.ofQuery(dataPath, s);
                String queryName = "Inline Query " + i;
                queries.put(queryName, dataQueryPath);
            } else {
                System.err.println("The value of the argument (" + i + ") is not file, a directory or a query");
                System.err.println("The argument value is: ");
                System.err.println(Strings.toStringNullSafe(s));
                CliUsage.print(cliCommand);
                System.exit(1);
            }
        }


        LOGGER.info("Processing the request");
        switch (queries.size()) {
            case 0:
                System.err.println();
                System.err.println("No query found");
                System.exit(1);
                break;

            case 1:

                // Prep
                CliTimer cliTimer = CliTimer.getTimer("execute").start();

                // Begin output
                DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.WARNING);
                System.out.println();
                Tabulars.print(queries.entrySet().iterator().next().getValue());
                System.out.println();
                DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);

                // Feedback
                cliTimer.stop();
                LOGGER.info("Response Time to query the data: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
                LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");
                break;

            default:

                TableDef executionTable = DataPaths.of("executions")
                        .getDataDef()
                        .addColumn("Query Name", Types.VARCHAR)
                        .addColumn("Latency (ms)", Types.INTEGER)
                        .addColumn("Row Count", Types.INTEGER)
                        .addColumn("Error", Types.VARCHAR)
                        .addColumn("Message", Types.VARCHAR);

                int errorCounter = 0;
                try (
                        InsertStream exeInput = Tabulars.getInsertStream(executionTable.getDataPath());
                ) {

                    for (DataPath queryDef : queries.values()) {

                        cliTimer = CliTimer.getTimer("execute").start();
                        Integer rowCount = null;
                        String status = "";
                        String message = "";
                        try {
                            rowCount = Tabulars.getSize(queryDef);
                        } catch (Exception e) {
                            errorCounter++;
                            status = "Err";
                            message = Log.onOneLine(e.getMessage());
                            LOGGER.severe(e.getMessage());
                        }

                        cliTimer.stop();
                        exeInput.insert(queryDef.getName(), cliTimer.getResponseTimeInMilliSeconds(), rowCount, status, message);

                    }
                }
                System.out.println();
                Tabulars.print(executionTable.getDataPath());
                System.out.println();

                if (errorCounter > 0) {
                    System.err.println(errorCounter + " Errors during Query executions were seen");
                    System.exit(1);
                }
                break;
        }

        LOGGER.info("Bye !");

    }

}
