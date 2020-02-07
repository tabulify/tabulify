package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DatastoreVault;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Relations;
import net.bytle.db.uri.SchemaDataUri;
import net.bytle.db.uri.TableDataUri;
import net.bytle.db.transfer.TransferManager;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.uri.IDataUri;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static net.bytle.db.cli.Words.DATASTORE_VAULT_PATH;
import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 * load data in a db
 */
public class DbTableLoad {

  private static final Log LOGGER = Db.LOGGER_DB_CLI;
  private static final String FILE_URI = "FILE_URIS";
  private static final String SCHEMA_URI = "SCHEMA_URI";
  private static final String TARGET_TABLE_NAMES = "TargetTableNames";

  public static void run(CliCommand cliCommand, String[] args) {

    // Create the command
    cliCommand.setDescription("Load one ore more files into a database.");
    cliCommand.optionOf(DATASTORE_VAULT_PATH);
    cliCommand.flagOf(NOT_STRICT)
      .setDescription("if set, it will not throw an error if a table is not found with the source table Uri")
      .setDefaultValue(false);
    cliCommand.argOf(SOURCE_DATA_URI)
      .setDescription("A source data uri pattern that represents one or more CSV file(s)")
      .setMandatory(true);
    cliCommand.argOf(TARGET_DATA_URI)
      .setDescription("A target data uri that represents the target table (Example: [name]@datastore). The name is optional and will be taken from the sources if not present")
      .setMandatory(true);

    // Args
    CliParser cliParser = Clis.getParser(cliCommand, args);

    // Source
    Path inputFilePath = null;
    String fileSourcePathArg = cliParser.getString(FILE_URI);
    int postPoint = fileSourcePathArg.lastIndexOf(".");
    if (postPoint == -1) {
      LOGGER.severe("The file must have an extension.");
      System.exit(1);
    } else {

      String fileExtension = fileSourcePathArg.substring(postPoint + 1).toLowerCase();

      if (fileExtension.equals("csv")) {
        inputFilePath = Paths.get(fileSourcePathArg);
      } else {
        LOGGER.severe("Actually only CSV files are supported (" + fileSourcePathArg + ").");
        CliUsage.print(cliCommand);
        System.exit(1);
      }
    }

    // Database Store
    final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
    DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);

    // Is there a path
    final IDataUri IDataUri = SchemaDataUri.of(cliParser.getString(SCHEMA_URI));
    final TableDataUri tableDataUri;
    if (IDataUri.getPathSegments().length > 0) {
      tableDataUri = TableDataUri.of(IDataUri);
    } else {
      Path fileName = inputFilePath.getFileName();
      String targetTableName = fileName.toString().substring(0, fileName.toString().lastIndexOf("."));
      LOGGER.info("The table name was not defined. The table name (" + targetTableName + ") was taken from the input file (" + fileName + ").");
      tableDataUri = TableDataUri.of(IDataUri.get(IDataUri.toString(), targetTableName));
    }
    Database targetDatabase = datastoreVault.getDataStore(tableDataUri.getDataStore());

    // Schema
    SchemaDef targetSchemaDef = targetDatabase.getCurrentSchema();
    if (tableDataUri.getSchemaName() != null) {
      LOGGER.info("The schema name (" + tableDataUri.getSchemaName() + ").");
      targetSchemaDef = targetDatabase.getSchema(tableDataUri.getSchemaName());
    } else {
      LOGGER.info("The schema name was not defined. The database default schema was taken (" + targetDatabase.getCurrentSchema() + ")");
    }

    // Target Table
    TableDef targetTable = targetSchemaDef.getTableOf(tableDataUri.getTableName());


    CliTimer cliTimer = CliTimer.getTimer(tableDataUri.getTableName())
      .start();

    LOGGER.info("Loading Table " + tableDataUri.getTableName());

    RelationDef relationDef = Relations.get(inputFilePath);

    List<TransferListener> resultSetListeners = new TransferManager(targetTable, relationDef)
      .targetWorkerCount(targetWorkerCount)
      .bufferSize(bufferSize)
      .batchSize(batchSize)
      .commitFrequency(commitFrequency)
      .metricsFilePath(metricsFilePath)
      .load();

    cliTimer.stop();

    LOGGER.info("Response Time for the load of the table (" + tableDataUri.getTableName() + ") with (" + targetWorkerCount + ") target workers: " + cliTimer.getResponseTime() + " (hour:minutes:seconds:milli)");
    LOGGER.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");

    int exitStatus = resultSetListeners.stream().mapToInt(s -> s.getExitStatus()).sum();
    if (exitStatus != 0) {
      LOGGER.severe("Error ! (" + exitStatus + ") errors were seen.");
      System.exit(exitStatus);
    } else {
      LOGGER.info("Success ! No errors were seen.");
    }


  }


}
