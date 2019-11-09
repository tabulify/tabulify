package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.DbDatabase.BYTLE_DB_DATABASES_STORE;
import static net.bytle.db.cli.Words.DATABASE_STORE;


/**
 * <p>
 */
public class DbDatabaseList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String DATABASE_PATTERN = "name|glob";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List the databases present in the store";

        // Create the parser
        cliCommand
                .setDescription(description);


        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(DATABASE_PATTERN)
                .setDescription("a sequence of database name or a glob pattern")
                .setMandatory(false)
                .setDefaultValue("*");

        cliCommand.optionOf(Words.DATABASE_STORE)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatabase.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_STORE);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        final List<String> names = cliParser.getStrings(DATABASE_PATTERN);

        final List<Database> databases = databasesStore.getDatabases(names);

        // Creating a table to use the print function
        TableDef databaseInfo = DataPaths.of("databases")
                .getDataDef()
                .addColumn("Name")
                .addColumn("Login")
                .addColumn("Password")
                .addColumn("Url")
                .addColumn("Driver");

        try (InsertStream tableInsertStream = Tabulars.getInsertStream(databaseInfo.getDataPath())) {
            for (Database database : databases) {
                String password = null;
                if (database.getPassword() != null) {
                    password = "xxx";
                }
                tableInsertStream
                        .insert(database.getDatabaseName(), database.getUser(), password, database.getUrl(), database.getDriver());
            }
        }


        System.out.println();
        Tabulars.print(databaseInfo.getDataPath());
        System.out.println();

        Tabulars.delete(databaseInfo.getDataPath());


        LOGGER.info("Bye !");


    }


}
