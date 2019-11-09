package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatabasesStore;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.cli.Words.DATABASE_STORE;


public class DbForeignKeyCount {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATA_URIS = "DataUri...";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand
                .setDescription("Count links (foreign keys)");

        cliCommand.argOf(DATA_URIS)
                .setDescription("One or more name table uri (ie @database[/schema]/table)")
                .setMandatory(true);

        cliCommand.optionOf(DATABASE_STORE);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        List<String> dataUris = cliParser.getStrings(DATA_URIS);
        List<ForeignKeyDef> foreignKeys = new ArrayList<>();

        for (String dataUri : dataUris) {

            DataPath tableDataUri = DataPaths.of(databasesStore, DataUri.of(dataUri));
            foreignKeys.addAll(tableDataUri.getDataDef().getForeignKeys());

        }

        System.out.println();
        if (foreignKeys.size() == 0) {

            System.out.println("No foreign key found");

        } else {

            System.out.println(foreignKeys.size() + " ForeignKeys");

        }
        System.out.println();
        LOGGER.info("Bye !");


    }

}
