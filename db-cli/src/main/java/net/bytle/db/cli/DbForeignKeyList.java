package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.MemorySelectStream;
import net.bytle.db.stream.Streams;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;

public class DbForeignKeyList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "tableName|pattern...";
    protected static final String SHOW_COLUMN = "c";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List foreign keys";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("Names of table or glob patterns")
                .setDefaultValue("*");

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);
        cliCommand.flagOf(SHOW_COLUMN)
                .setDescription("Show also the columns if present");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET);

        /**
         * Within a test, the url of the database may have been set
         * Because the option have a sqlite default, the setting will cause an error
         */
        if (database.getUrl() == null) {
            database.setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                    .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));
        }


        List<String> patterns = cliParser.getStrings(ARG_NAME);
        List<ForeignKeyDef> foreignKeys = new ArrayList<>();
        for (String pattern : patterns) {
            final SchemaDef currentSchema = database.getCurrentSchema();
            final List<ForeignKeyDef> foreignKeys1 = currentSchema.getForeignKeys(pattern);
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
                    .addColumn("Child Table")
                    .addColumn("<-")
                    .addColumn("Parent Table")
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
