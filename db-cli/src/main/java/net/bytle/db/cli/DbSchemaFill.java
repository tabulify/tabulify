package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DbLoggers;
import net.bytle.db.gen.DataGenLoader;
import net.bytle.db.gen.yml.DataGenYml;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbSchemaFill {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    protected final static String ARG = "(Schema|DataGen.yml)";

    private static final Integer LOAD_CURRENT_SCHEMA = 1;
    private static final Integer LOAD_WITH_YAML = 2;

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Load generated data into the tables of a schema";


        // Create the command
        cliCommand
                .setDescription(description);
        cliCommand.argOf(ARG)
                .setDescription("A schema or a data definition file (Default: the current schema)");
        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        // Parser
        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Yaml file
        final Path yamlFile = cliParser.getPath(ARG);
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
        String url = cliParser.getString(JDBC_URL_TARGET_OPTION);
        String driver = cliParser.getString(JDBC_DRIVER_TARGET_OPTION);
        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(url)
                .setDriver(driver);

        CliTimer cliTimer = CliTimer.getTimer("schema").start();

        if (yamlFile != null) {

            LOGGER.info("Loading generated data with the file " + yamlFile);
            InputStream input = null;
            try {
                input = Files.newInputStream(yamlFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DataGenYml dataGenYml = new DataGenYml(database, input).loadParentTable(true);
            List<TableDef> tables = dataGenYml.load();

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
                    DataGenLoader.get(tableDef)
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
