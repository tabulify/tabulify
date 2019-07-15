package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasePath;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;


public class DbTableDescribe {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATABASE_PATH = "(name|pattern)..";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Show the structure of a table";


        // The arguments
        cliCommand
                .setDescription(description);
        cliCommand.argOf(DATABASE_PATH)
                .setDescription("One ore more database paths")
                .setMandatory(true);
        cliCommand.optionOf(STORAGE_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        //
        List<String> databasePaths = cliParser.getStrings(DATABASE_PATH);
        for (String databasePathString :databasePaths){
            DatabasePath databasePath = DatabasePath.of(databasePathString);
            Database database = databasesStore.getDatabase(databasePath.getDatabaseName());

            SchemaDef schemaDef;
            if (databasePath.getSchemaName()!=null) {
                schemaDef = database.getSchema(databasePath.getSchemaName());
            } else {
                schemaDef = database.getCurrentSchema();
            }
            List<TableDef> tableDefList = schemaDef.getTables(databasePath.getDestinationPart());
            System.out.println();

            if (tableDefList.size() != 0) {

                switch (tableDefList.size()) {
                    case 1:
                        Tables.printColumns(tableDefList.get(0));
                        break;
                    default:

                        for (TableDef tableDef : tableDefList) {
                            System.out.println();
                            System.out.println("  * " + tableDef.getName() + " columns:");
                            Tables.printColumns(tableDef);
                        }


                }
            } else {

                System.out.println("No tables found");

            }
            System.out.println();
        }



        LOGGER.info("Bye !");


    }


}
