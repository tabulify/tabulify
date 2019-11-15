package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.DatabasesStore;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATABASE_STORE;


public class DbForeignKeyList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URIS = "TableUri...";
    protected static final String SHOW_COLUMN = "c";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List the relationships";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more name table uri (ie glob@datastore")
                .setMandatory(true);

        cliCommand.optionOf(DATABASE_STORE);

        cliCommand.flagOf(SHOW_COLUMN)
                .setDescription("Show also the columns if present");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);


        List<ForeignKeyDef> foreignKeys = new ArrayList<>();

        for (String stringTableUri : cliParser.getStrings(TABLE_URIS)) {
            DataUri dataUri = DataUri.of(stringTableUri);
            foreignKeys.addAll(DataPaths.select(databasesStore, dataUri)
                    .stream()
                    .flatMap(d -> d.getDataDef().getForeignKeys().stream())
                    .collect(Collectors.toList()));
        }


        if (foreignKeys.size() == 0) {

            System.out.println("No relation found");

        } else {

            // Sorting them ascending
            Collections.sort(foreignKeys);

            // Creating a table to use the print function
            DataPath foreignKeysInfo = DataPaths.of("foreignKeys")
                    .getDataDef()
                    .addColumn("Id", Types.INTEGER)
                    .addColumn("Child/Foreign Table")
                    .addColumn("<-")
                    .addColumn("Parent/Primary Table")
                    .addColumn("From Foreign Key")
                    .getDataPath();

            Boolean showColumns = cliParser.getBoolean(SHOW_COLUMN);

            try (
                    InsertStream insertStream = Tabulars.getInsertStream(foreignKeysInfo)
            ) {
                // Filling the table with data
                Integer fkNumber = 0;
                for (ForeignKeyDef foreignKeyDef : foreignKeys) {
                    final String[] nativeColumns = foreignKeyDef.getChildColumns().stream()
                            .map(ColumnDef::getColumnName)
                            .collect(Collectors.toList())
                            .toArray(new String[foreignKeyDef.getChildColumns().size()]);
                    final String childCols = String.join(",", nativeColumns);
                    final String[] pkColumns = foreignKeyDef.getForeignPrimaryKey().getColumns().stream()
                            .map(ColumnDef::getColumnName)
                            .collect(Collectors.toList())
                            .toArray(new String[foreignKeyDef.getForeignPrimaryKey().getColumns().size()]);

                    String parentCols = String.join(",", pkColumns);
                    fkNumber++;

                   insertStream.insert(
                            fkNumber,
                            foreignKeyDef.getTableDef().getDataPath().getName() + (showColumns ? " (" + childCols + ")" : ""),
                            "<-",
                            foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath().getName() + (showColumns ? " (" + parentCols + ")" : ""),
                            foreignKeyDef.getName()
                    );

                }
            }

            // Printing
            System.out.println();
            System.out.println("ForeignKeys:");
            Tabulars.print(foreignKeysInfo);

            if (!showColumns) {
                System.out.println();
                System.out.println("Tip: You can show the columns by adding the c flag.");
            }
            // In the test we run it twice,
            // we will then insert the data twice
            // We need to suppress the data
            Tabulars.delete(foreignKeysInfo);


        }
        System.out.println();
        LOGGER.info("Bye !");


    }

}
