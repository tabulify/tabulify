package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.Tabular;
import net.bytle.db.gen.DataGeneration;
import net.bytle.db.gen.DataGens;
import net.bytle.db.gen.GenDataPath;
import net.bytle.db.gen.fs.GenFsDataPath;
import net.bytle.db.model.DataDefs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.fs.Fs;
import net.bytle.timer.Timer;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.NOT_STRICT;


/**
 *
 * load data in a schema
 */
public class DbSchemaFill {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbSchemaFill.class);

  protected final static String SCHEMA_URI = "SchemaUri";
  protected final static String WITH_DEPENDENCIES = "with-dependencies";


  public static void run(CliCommand cliCommand, String[] args) {

    // Create the command
    cliCommand
      .setDescription("Load generated data into the tables of a schema");
    cliCommand.addExample(Strings.multiline(
      "Load all data generation unit into a sqlite database",
        CliUsage.getFullChainOfCommand(cliCommand)+" *"+ GenFsDataPath.EXTENSION+" @sqlite"
    ));
    cliCommand.argOf(Words.GLOB_PATTERN_DATADEF_FILE)
      .setDescription("A glob pattern that defines one or more data generation file (ie dir/*"+ GenFsDataPath.EXTENSION+")");
    cliCommand.argOf(SCHEMA_URI)
      .setDescription("A Data Uri that points to a schema (example for the default one `@datastore`")
      .setMandatory(true);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error for a minor problem (example if a data def has not a yml extension,...) ")
      .setDefaultValue(false);
    cliCommand.flagOf(WITH_DEPENDENCIES)
      .setDescription("if set, it will load also the dependencies with random generated data (ie foreign tables)")
      .setDefaultValue(false);
    cliCommand.optionOf(DATASTORE_VAULT_PATH);

    // Parser and args
    CliParser cliParser = Clis.getParser(cliCommand, args);
    final String dataDefGlobArg = cliParser.getString(Words.GLOB_PATTERN_DATADEF_FILE);
    final Boolean notStrictRunArg = cliParser.getBoolean(NOT_STRICT);
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES);
    final Path storagePathValueArg = cliParser.getPath(DATASTORE_VAULT_PATH);
    final String targetSchemaDataUriArg = cliParser.getString(SCHEMA_URI);

    // Args control
    List<Path> dataDefsPaths;
    if (dataDefGlobArg != null) {
      dataDefsPaths = Fs.getFilesByGlob(dataDefGlobArg);
      dataDefsPaths.forEach(yamlFile -> {
        String fileExtension = Fs.getExtension(yamlFile.getFileName().toString());
        if (!fileExtension.toLowerCase().equals("yml")) {
          String msg = "A definition file must have a yml extension. The file (" + yamlFile + ") has not a yml extension.";
          if (notStrictRunArg) {
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

      if (storagePathValueArg != null) {
        tabular.setDataStoreVault(storagePathValueArg);
      } else {
        tabular.withDefaultDataStoreVault();
      }

      // Target
      DataPath targetDataPath = tabular.getDataPath(targetSchemaDataUriArg);
      if (!Tabulars.isContainer(targetDataPath)) {
        String msg = "The target data uri (" + targetSchemaDataUriArg + ") is not a schema";
        if (notStrictRunArg) {
          LOGGER.warn(msg);
        } else {
          LOGGER.error(msg);
          System.exit(1);
        }
      }

      List<DataPath> dataPaths = Tabulars.getChildren(targetDataPath);
      if (dataPaths.size() == 0) {
        String msg = "The target schema defined by the data uri (" + targetSchemaDataUriArg + ") seems to have no data paths (tables)";
        if (notStrictRunArg) {
          LOGGER.warn(msg);
          System.exit(0);
        } else {
          LOGGER.error(msg);
          System.exit(1);
        }
      }

      List<Path> dataGenYmls = null;
      if (dataDefGlobArg != null) {

        dataGenYmls = Fs.getFilesByGlob(dataDefGlobArg);
        if (dataGenYmls.size() == 0) {
          String msg = "The data def glob pattern (" + dataDefGlobArg + ") has selected no data definition files";
          if (notStrictRunArg) {
            LOGGER.warn(msg);
          } else {
            LOGGER.error(msg);
            System.exit(1);
          }
        } else {

          Map<String, DataPath> dataPathDataGen = dataGenYmls
            .stream()
            .map(tabular::getDataPathOfDataDef)
            .collect(Collectors.toMap(DataPath::getName, Function.identity()));

          // Merge
          dataPaths.stream()
            .filter(dp -> dataPathDataGen.keySet().contains(dp.getName()))
            .forEach(dp -> {
                DataDefs.mergeProperties(dp.getOrCreateDataDef(), dataPathDataGen.get(dp.getName()).getOrCreateDataDef());
              }
            );

        }
      }

      // Data Generation object build
      DataGeneration dataGeneration = DataGeneration.of()
        .addTables(dataPaths);

      // Feedback message building
      String withOrWithoutDependencies = "without";
      if (withDependencies) {
        dataGeneration.loadDependencies(true);
        withOrWithoutDependencies = "and";
      }
      String withOrWithoutDataDef = "without data definitions ";
      if (dataGenYmls!=null){
        withOrWithoutDataDef="with ("+dataGenYmls.size()+") data definitions defined by the glob pattern (" + dataDefGlobArg + ") ";
      }
      LOGGER.info("Loading generated data "+withOrWithoutDataDef + withOrWithoutDependencies + " the dependencies (foreign tables)");

      // Start loading
      Timer cliTimer = Timer.getTimer("schema fill").start();
      List<DataPath> loadedDataPaths = dataGeneration
        .load();
      cliTimer.stop();

      // Feedback
      LOGGER.info("Response Time for the loading of generated data : " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
      LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds%n");


      LOGGER.info("The following tables where loaded:");
      for (DataPath dataPath : loadedDataPaths) {
        LOGGER.info("  * " + dataPath + ", Size (" + Tabulars.getSize(dataPath) + ")");
      }

      LOGGER.info("Success ! No errors were seen.");

    }

  }
}
