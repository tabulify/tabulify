package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.MemorySelectStream;
import net.bytle.db.stream.Streams;

import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATABASE_STORE;


public class DbForeignKeyList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URIS = "TableUri...";
    protected static final String SHOW_COLUMN = "c";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List foreign keys";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more name table uri (ie @database[/schema]/table)")
                .setMandatory(true);

        cliCommand.optionOf(DATABASE_STORE);

        cliCommand.flagOf(SHOW_COLUMN)
                .setDescription("Show also the columns if present");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        List<String> stringTableUris = cliParser.getStrings(TABLE_URIS);
        List<ForeignKeyDef> foreignKeys = new ArrayList<>();

        for (String stringTableUri : stringTableUris) {
            TableDataUri tableDataUri = TableDataUri.of(stringTableUri);
            Database database = databasesStore.getDatabase(tableDataUri.getDataStore());
            SchemaDef schemaDef = database.getCurrentSchema();
            if (tableDataUri.getSchemaName()!=null) {
                schemaDef = database.getSchema(tableDataUri.getSchemaName());
            }
            final List<ForeignKeyDef> foreignKeys1 = schemaDef.getForeignKeys(tableDataUri.getTableName());
            foreignKeys.addAll(foreignKeys1);

        }


        if (foreignKeys.size() == 0) {

            System.out.println("No foreign key found");

        } else {

            // Sorting them ascending
            foreignKeys = foreignKeys.stream()
                    .sorted(Comparator.comparing(s -> (s.getTableDef().getName() + s.getForeignPrimaryKey().getTableDef().getName())))
                    .collect(Collectors.toList());

            // Creating a table to use the print function
            TableDef foreignKeysInfo = Tables.get("foreignKeys")
                    .addColumn("Id", Types.INTEGER)
                    .addColumn("Child/Foreign Table")
                    .addColumn("<-")
                    .addColumn("Parent/Primary Table")
                    .addColumn("From Foreign Key");

            Boolean showColumns = cliParser.getBoolean(SHOW_COLUMN);

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
                Tables.getTableInsertStream(foreignKeysInfo)
                        .insert(
                                fkNumber,
                                foreignKeyDef.getTableDef().getName() + (showColumns ? " (" + childCols + ")" : ""),
                                "<-",
                                foreignKeyDef.getForeignPrimaryKey().getTableDef().getName() + (showColumns ? " (" + parentCols + ")" : ""),
                                foreignKeyDef.getName()
                        );

            }

            // Printing
            System.out.println();
            System.out.println("ForeignKeys:");
            MemorySelectStream tableOutputStream = Tables.getTableOutputStream(foreignKeysInfo);
            Streams.print(tableOutputStream);

            if (!showColumns) {
                System.out.println();
                System.out.println("Tip: You can show the columns by adding the c flag.");
            }
            // In the test we run it twice,
            // we will then insert the data twice
            // We need to suppress the data
            Tables.delete(foreignKeysInfo);


        }
        System.out.println();
        LOGGER.info("Bye !");


    }

}
