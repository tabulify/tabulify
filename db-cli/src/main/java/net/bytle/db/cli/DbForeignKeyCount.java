package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.cli.Log;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;

import java.util.ArrayList;
import java.util.List;


public class DbForeignKeyCount {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "tableName|pattern...";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Count links (foreign keys)";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("Names of table or regular expression patterns")
                .setDefaultValue(".*");


        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET);

        List<String> patterns = cliParser.getStrings(ARG_NAME);
        List<ForeignKeyDef> foreignKeys = new ArrayList<>();
        for (String pattern : patterns) {
            final SchemaDef currentSchema = database.getCurrentSchema();
            final List<ForeignKeyDef> foreignKeys1 = currentSchema.getForeignKeys(pattern);
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
