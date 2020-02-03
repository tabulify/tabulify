package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.log.Log;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.Database;
import net.bytle.db.model.DataDefs;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;


public class DbTableDescribe {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String DATABASE_PATH = "(name|pattern)..";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Show the structure of a table";


        // The arguments
        cliCommand
                .setDescription(description);
        cliCommand.argOf(DATABASE_PATH)
                .setDescription("One ore more table data uri (@database[/schema]/table")
                .setMandatory(true);
        cliCommand.optionOf(DATASTORE_VAULT_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

        //
        List<String> databasePaths = cliParser.getStrings(DATABASE_PATH);
        for (String databasePathString :databasePaths){
            TableDataUri databasePath = TableDataUri.of(databasePathString);
            Database database = datastoreVault.getDataStore(databasePath.getDataStore());

            SchemaDef schemaDef;
            if (databasePath.getSchemaName()!=null) {
                schemaDef = database.getSchema(databasePath.getSchemaName());
            } else {
                schemaDef = database.getCurrentSchema();
            }
            List<TableDef> tableDefList = schemaDef.getTables(databasePath.getTableName());
            System.out.println();

            if (tableDefList.size() != 0) {

                switch (tableDefList.size()) {
                    case 1:
                        net.bytle.db.model.DataDefs.printColumns(tableDefList.get(0));
                        break;
                    default:

                        for (TableDef tableDef : tableDefList) {
                            System.out.println();
                            System.out.println("  * " + tableDef.getName() + " columns:");
                            DataDefs.printColumns(tableDef);
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
