package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;

import net.bytle.db.engine.Queries;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.model.QueryDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;


public class DbTableShow {

    public static final Log LOGGER_DB_ENGINE = DbLoggers.LOGGER_DB_ENGINE;
    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URI = "TableUri..";

    public static void run(CliCommand cliCommand, String[] args) {


        String description = "Show the data of a table";


        // Create the parser
        cliCommand
                .setDescription(description);
        cliCommand.argOf(TABLE_URI)
                .setDescription("A table URI (@database[/schema]/table")
                .setMandatory(true);

        cliCommand.optionOf(STORAGE_PATH);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // Timer
        CliTimer cliTimer = CliTimer.getTimer("execute").start();
        LOGGER_DB_ENGINE.setLevel(Level.WARNING);
        System.out.println();

        // Start
        List<String> tableURIs = cliParser.getStrings(TABLE_URI);
        for (String tableUri: tableURIs) {
            TableDataUri tableDataUri = TableDataUri.of(tableUri);
            List<Database> databases = databasesStore.getDatabases(tableDataUri.getDatabaseName());
            for (Database database: databases) {
                //TODO: The schema is not taken from the table uri
                List<TableDef> tableDefList = database.getCurrentSchema().getTables(tableDataUri.getTableName());
                System.out.println();
                if (tableDefList.size() != 0) {

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


                } else {

                    System.out.println("No tables found in the database ("+database+").");

                }

            }
        }

        // Feedback
        cliTimer.stop();
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        LOGGER.info("Response Time to query the data: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");

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
