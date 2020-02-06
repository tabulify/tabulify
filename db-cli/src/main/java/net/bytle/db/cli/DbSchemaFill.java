package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.database.Database;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.engine.Tables;
import net.bytle.db.gen.DataGeneration;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.fs.Fs;
import net.bytle.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbSchemaFill {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbSchemaFill.class);
  ;
  protected final static String SCHEMA_URI = "SchemaUri";


  public static void run(CliCommand cliCommand, String[] args) {

    // Create the command
    cliCommand
      .setDescription("Load generated data into the tables of a schema");
    cliCommand.argOf(SCHEMA_URI)
      .setDescription("A Data Uri that points to a schema (example for the default one `@datastore`")
      .setMandatory(true);
    cliCommand.optionOf(Words.GLOB_PATERN_DATADEF_FILE)
      .setDescription("A glob pattern that defines data definition file with the data generation properties (ie dir/*--datadef.yml)");
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error for minor problem (example if a data def has not a yml extension,...) ")
      .setDefaultValue(false);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Parser and args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final String dataDefGlob = cliParser.getString(Words.GLOB_PATERN_DATADEF_FILE);
    final Boolean notStrictRun = cliParser.getBoolean(NOT_STRICT);
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String schemaUri = cliParser.getString(SCHEMA_URI);

    // Args control
    List<Path> dataDefsPaths = new ArrayList<>();
    if (dataDefGlob != null) {
      dataDefsPaths = Fs.getFilesByGlob(dataDefGlob);
      dataDefsPaths.forEach(yamlFile -> {
        String fileExtension = Fs.getExtension(yamlFile.getFileName().toString());
        if (!fileExtension.toLowerCase().equals("yml")) {
          String msg = "A definition file must have a yml extension. The file (" + yamlFile + ") has not a yml extension.";
          if (notStrictRun) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            CliUsage.print(cliCommand);
            System.exit(1);
          }
        }
      });
    }


    // Main
    try (Tabular tabular = Tabular.tabular()) {

      if (storagePathValue != null) {
        tabular.setDataStoreVault(storagePathValue);
      } else {
        tabular.withDefaultStorage();
      }

      List<DataPath> dataPaths = DataDefs.load(dataGenYml)
        .stream()
        .map(dataDefDataPath -> {
          final JdbcDataPath childDataPath = jdbcDataSystem.getDataPath(dataDefDataPath.getName());
          DataDefs.mergeProperties(childDataPath.getDataDef(), dataDefDataPath.getDataDef());
          return childDataPath;
        })
        .collect(Collectors.toList());

      Timer cliTimer = Timer.getTimer("schema").start();

      if (yamlFile != null) {

        LOGGER.info("Loading generated data from the path (" + yamlFile + ")");
        List<TableDef> dataGenDefs = DataDefs.of().load(yamlFile);

        List<DataPath> tables = DataGeneration.of()
          .addTables(dataGenDefs)
          .loadDependencies(true)
          .load();

        LOGGER.info("The following tables where loaded:");
        for (TableDef tableDef : tables) {
          LOGGER.info("  * " + tableDef.getFullyQualifiedName() + ", Size (" + Tables.getSize(tableDef) + ")");
        }

      } else {

        SchemaDef currentSchema = database.getCurrentSchema();
        LOGGER.info("Loading generated data to the schema (" + currentSchema.getName() + ") without file definition.");

        List<TableDef> tableDefs = ForeignKeyDag.get(currentSchema).getCreateOrderedTables();
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
