package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.cli.Words.DATABASE_STORE;


public class DbForeignKeyCount {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String TABLE_URIS = "tableUri...";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand
                .setDescription("Count links (foreign keys)");

        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more name table uri (ie @database[/schema]/table)")
                .setMandatory(true);

        cliCommand.optionOf(DATABASE_STORE);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        List<String> stringTableUris = cliParser.getStrings(TABLE_URIS);
        List<ForeignKeyDef> foreignKeys = new ArrayList<>();

        for (String stringTableUri : stringTableUris) {
            TableDataUri tableDataUri = TableDataUri.of(stringTableUri);
            Database database = databasesStore.getDatabase(tableDataUri.getDatabaseName());
            SchemaDef schemaDef = database.getCurrentSchema();
            if (tableDataUri.getSchemaName()!=null) {
                schemaDef = database.getSchema(tableDataUri.getSchemaName());
            }
            final List<ForeignKeyDef> foreignKeys1 = schemaDef.getForeignKeys(tableDataUri.getTableName());
            foreignKeys.addAll(foreignKeys1);

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
