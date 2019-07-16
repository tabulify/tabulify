package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.TableDataUri;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static java.lang.System.exit;
import static net.bytle.db.cli.DbDatabase.STORAGE_PATH;


public class DbTableDrop {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    public static final String NO_STRICT = "nostrict";
    public static final String FORCE = "force";
    private static final String TABLE_URIS = "tableUri...";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Drop table(s).";
        String example = "";
        example += "To drop the tables D_TIME and F_SALES:\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + "@database/D_TIME F_SALES\n\n";
        example += "To drop only the table D_TIME with force (ie deleting the foreign keys constraint):\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + CliParser.PREFIX_LONG_OPTION + FORCE + "@database/D_TIME\n\n";
        example += "To drop all dimension tables that begins with (D_):\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + "\"^D\\_.*\"\n\n";
        example += "To drop all tables:\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + " \"@database/*\"\n\n";

        // Create the parser
        cliCommand
                .setDescription(description)
                .setExample(example);


        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more table URI")
                .setMandatory(true);

        cliCommand.flagOf(NO_STRICT)
                .setDescription("if set, it will not throw an error if a table is not found")
                .setDefaultValue(false);

        cliCommand.flagOf(FORCE)
                .setDescription("if set, the table will be dropped even if referenced by a foreign constraint");

        cliCommand.optionOf(STORAGE_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(STORAGE_PATH);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);


        // Bring the get statement out of the output zone
        // Otherwise we will not see them their log in the output stream
        final Boolean withForce = cliParser.getBoolean(FORCE);
        final Boolean notStrict = cliParser.getBoolean(NO_STRICT);


        // Get the tables asked
        List<String> tableUris = cliParser.getStrings(TABLE_URIS);
        List<TableDef> tables = new ArrayList<>();
        for (String tableUri : tableUris) {
            TableDataUri tableDataUri = TableDataUri.of(tableUri);
            Database database = databasesStore.getDatabase(tableDataUri.getDatabaseName());
            List<SchemaDef> schemaDefs = database.getSchemas(tableDataUri.getSchemaName());
            if (schemaDefs.size()==0){
                schemaDefs = Arrays.asList(database.getCurrentSchema());
            }
            for (SchemaDef schemaDef: schemaDefs) {
                List<TableDef> tablesFound = schemaDef.getTables(tableDataUri.getTableName());
                if (tablesFound.size() != 0) {

                    tables.addAll(tablesFound);

                } else {

                    final String msg = "No tables found with the name/pattern (" + tableUri + ")";
                    if (notStrict) {

                        LOGGER.warning(msg);

                    } else {

                        LOGGER.severe(msg);
                        exit(1);


                    }

                }
            }

        }

        if (tables.size()==0){
            LOGGER.warning("No tables found to drop");
            if (notStrict){
                return;
            } else {
                System.exit(1);
            }
        }

        // Doing the work
        System.out.println();
        Dag dag = Dag.get(tables);
        for (TableDef tableDef : dag.getDropOrderedTables()) {

            if (withForce) {
                List<ForeignKeyDef> foreignKeys = tableDef.getExternalForeignKeys();
                for (ForeignKeyDef foreignKeyDef : foreignKeys) {
                    Tables.dropForeignKey(foreignKeyDef);
                    System.out.println("ForeignKey (" + foreignKeyDef.getName() + ") was dropped from the table (" + foreignKeyDef.getTableDef().getFullyQualifiedName() + ")");
                }
            }
            Tables.drop(tableDef);
            System.out.println("Table (" + tableDef.getFullyQualifiedName() + ") was dropped.");
        }

        // End
        System.out.println();
        // Setting the log back to see them in a test
        DbLoggers.LOGGER_DB_ENGINE.setLevel(Level.INFO);
        LOGGER.info("Bye !");

    }


}
