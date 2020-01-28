package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.gen.DataGeneration;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.log.Log;
import net.bytle.timer.Timer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbSchemaFill {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    protected final static String SCHEMA_URI = "SchemaUri";



    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Load generated data into the tables of a schema";


        // Create the command
        cliCommand
                .setDescription(description);
        cliCommand.argOf(SCHEMA_URI)
                .setDescription("A schema Uri (ie @database[/schema]");


        cliCommand.optionOf(Words.DEFINITION_FILE)
                .setDescription("A path to a data definition file (DataDef.yml) or a directory containing several data definition file.");


        // Parser
        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Yaml file
        final Path yamlFile = cliParser.getPath(SCHEMA_URI);
        if (yamlFile != null) {

            String fileSource = yamlFile.toString();
            int postPoint = fileSource.lastIndexOf(".");
            if (postPoint == -1) {
                System.err.println("The file (" + yamlFile.normalize().toString() + ") must have an (yml) extension");
                System.exit(1);
            } else {
                String fileExtension = fileSource.substring(postPoint + 1, fileSource.length());
                if (!fileExtension.toLowerCase().equals("yml")) {
                    LOGGER.severe("A definition file must have a yml extension (" + yamlFile + ").");
                    CliUsage.print(cliCommand);
                    System.exit(1);
                }
            }

        }


        // Database
        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET);

        Timer cliTimer = Timer.getTimer("schema").start();

        if (yamlFile != null) {

            LOGGER.info("Loading generated data from the path (" + yamlFile+")");
            List<TableDef> dataGenDefs = DataDefs.of().load(yamlFile);

            List<TableDef> tables = DataGeneration.of()
                    .addTables(dataGenDefs)
                    .loadParentTable(true)
                    .load();

            LOGGER.info("The following tables where loaded:");
            for (TableDef tableDef : tables) {
                LOGGER.info("  * " + tableDef.getFullyQualifiedName() + ", Size (" + Tables.getSize(tableDef) + ")");
            }

        } else {

            SchemaDef currentSchema = database.getCurrentSchema();
            LOGGER.info("Loading generated data to the schema (" + currentSchema.getName() + ") without file definition.");

            List<TableDef> tableDefs = Dag.get(currentSchema).getCreateOrderedTables();
            if (tableDefs.size() > 0) {

                // List
                for (TableDef tableDef : tableDefs) {
                    List<ForeignKeyDef> foreignKeys = tableDef.getForeignKeys();
                    String parentTableDef = "no parent";
                    if (foreignKeys.size() > 0) {
                        List<String> foreignPrimaryKeys = new ArrayList<>();
                        for (ForeignKeyDef foreignKeyDef : foreignKeys) {
                            foreignPrimaryKeys.add(foreignKeyDef.getForeignPrimaryKey().getTableDef().getName());
                        }
                        parentTableDef = String.join(",", foreignPrimaryKeys) + " as parent";
                    }
                    LOGGER.fine(tableDef.getFullyQualifiedName() + " has " + parentTableDef);
                }

                // Loading
                for (TableDef tableDef : tableDefs) {
                    DataGeneration.of()
                            .addTable(tableDef)
                            .load();
                }
            } else {
                LOGGER.warning("No table to load in the schema (" + currentSchema + ")");
            }
        }

        cliTimer.stop();
        LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
        LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");
        LOGGER.info("Success ! No errors were seen.");

    }


}
