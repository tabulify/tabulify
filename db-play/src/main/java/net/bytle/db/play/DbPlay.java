package net.bytle.db.play;

import net.bytle.db.DatastoreVault;
import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.jdbc.SqlDataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.type.Maps;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A play object
 */
public class DbPlay {

  private Path playFile;
  private Path homeDirectory;
  private Tabular tabular;


  /**
   * The play file and the play home
   *
   * @throws IOException
   */
  public void run() throws IOException {

    Path dsnDir = homeDirectory.resolve("dsn");
    tabular = Tabular.tabular()
      .setDataStoreVault(dsnDir.resolve("dsn.ini"));

    // The Yaml entry class
    Yaml yaml = new Yaml();

    // Load the yaml documents from the file
    List<List<Map<String, Object>>> documents = new ArrayList<>();
    for (Object data : yaml.loadAll(Files.newInputStream(playFile))) {
      List<Map<String, Object>> document;
      try {
        document = (List<Map<String, Object>>) data;
      } catch (ClassCastException e) {
        String message = "A data Def must be in a list format. ";
        if (data.getClass().equals(java.util.ArrayList.class)) {
          message += "They are in a list format. You should suppress the minus if they are present.";
        }
        message += "The Bad Data Def Values are: " + data;
        throw new RuntimeException(message, e);
      }
      documents.add(document);
    }

    // We expect one document
    switch (documents.size()) {
      case 0:
        throw new RuntimeException("There is nothing to do because we couldn't find any yaml content found in the file " + playFile);
      case 1:

        List<Map<String, Object>> document = documents.get(0);
        String currentTaskName = null;
        // Loop through all the task
        for (Map<String, Object> task : document) {
          String taskName = (String) Maps.getPropertyCaseIndependent(task, "task");
          if (taskName == null) {
            String desc = currentTaskName == null ? "the first task" : "the task after the task (" + currentTaskName + ")";
            throw new RuntimeException("The task property is mandatory and was not found for " + desc);
          }
          currentTaskName = taskName;
          switch (currentTaskName) {
            case "clean":
              cleanTask(task);
              break;
            case "load":
              loadTask(task);
              break;
            case "sql":
              sqlTask(task);
              break;
            default:
              throw new RuntimeException("Task " + currentTaskName + " is unknown");
          }
        }

        break;
      default:
        throw new RuntimeException("Too much metadata documents (" + documents.size() + ") found in the file (" + playFile.toString() + ") ");
    }
  }

  private void cleanTask(Map<String, Object> task) {

    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the clean operations: " + desc);
    String target = (String) Maps.getPropertyCaseIndependent(task, "target");
    DataStore dataStore = this.tabular.getDataStore(target);

    DataPath dataPath = dataStore.getCurrentDataPath();
    List<DataPath> children = Tabulars.getChildren(dataPath);
    ForeignKeyDag.get(children).getDropOrderedTables().stream().forEach(Tabulars::drop);
    System.out.println("The clean operations has succeeded and has deleted the following tables " + children);

  }

  private void loadTask(Map<String, Object> task) {

    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the load operations: " + desc);

    // Source
    SqlDataStore jdbcDataStore;
    String targetPath;
    DataPath sourceDataPath;
    Path sourcePath = null;
    Object sourceValue = Maps.getPropertyCaseIndependent(task, "source");
    DataPath source;
    if (sourceValue instanceof String) {
      source = this.tabular.getDataPath((String) sourceValue);
    } else {
      throw new RuntimeException("The source is not a string but a "+sourceValue.getClass().toString());
    }


    if (sourceValue.getClass().equals(String.class)) {
      // A file
      sourcePath = dataDir.resolve((String) sourceValue);
      sourceDataPath = DataPaths.of(sourcePath);
      if (sourceDataPath.getClass().equals(CsvDataPath.class)) {
        sourceDataPath = ((CsvDataPath) sourceDataPath)
          .getDataDef()
          .setHeaderRowCount(1)
          .getDataPath();
      }
    } else {
      // A query
      Map<String, Object> targetValues = (Map<String, Object>) sourceValue;
      String dsn = (String) Maps.getPropertyCaseIndependent(targetValues, "dsn");
      jdbcDataStore = datastoreVault.getDataStore(dsn);
      targetPath = (String) Maps.getPropertyCaseIndependent(targetValues, "sql");
      Path targetSqlFile = sqlDir.resolve(targetPath);
      sourceDataPath = DataPaths.ofQuery(jdbcDataStore, Fs.getFileContent(targetSqlFile));
    }

    // Target
    Object targetValue = Maps.getPropertyCaseIndependent(task, "target");
    DataPath targetDataPath;
    if (targetValue.getClass().equals(String.class)) {
      jdbcDataStore = datastoreVault.getDataStore((String) targetValue);
      String sourceFileName = sourcePath.getFileName().toString();
      targetPath = Fs.getFileNameWithoutExtension(sourceFileName);
      targetDataPath = DataPaths.of(jdbcDataStore, targetPath);
    } else {
      Map<String, Object> targetValues = (Map<String, Object>) targetValue;
      String dsn = (String) Maps.getPropertyCaseIndependent(targetValues, "dsn");
      jdbcDataStore = datastoreVault.getDataStore(dsn);
      String targetPathValue = (String) Maps.getPropertyCaseIndependent(targetValues, "name");
      if (dsn.equals("file")) {
        Path targetValueAsPath = workingDir.resolve(targetPathValue);
        targetDataPath = DataPaths.of(targetValueAsPath);
      } else {
        targetDataPath = DataPaths.of(jdbcDataStore, targetPathValue);
      }
    }


    Tabulars.transfer(sourceDataPath, targetDataPath);
    System.out.println("The load operations has terminated");
  }

  private void sqlTask(Map<String, Object> task) {
    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the sql operations: " + desc);
    String sourceValue = (String) Maps.getPropertyCaseIndependent(task, "source");
    DatastoreVault datastoreVault = DatastoreVault.of(dataDir.resolve("dsn.ini"));
    SqlDataStore sourceJdbcDataStore = datastoreVault.getDataStore(sourceValue);
    Object targetValue = Maps.getPropertyCaseIndependent(task, "target");
    String targetDsn = null;
    String targetName = null;
    if (targetValue instanceof Map) {
      Map<String, Object> targetMap = (Map<String, Object>) targetValue;
      targetDsn = (String) Maps.getPropertyCaseIndependent(targetMap, "dsn");
      targetName = (String) Maps.getPropertyCaseIndependent(targetMap, "name");
    }
    SqlDataStore targetJdbcDataStore = datastoreVault.getDataStore(targetDsn);
    String sqlValue = (String) Maps.getPropertyCaseIndependent(task, "sql");
    if (targetName == null) {
      targetName = Fs.getFileNameWithoutExtension(sqlValue);
    }
    DataPath targetDataPath = DataPaths.of(targetJdbcDataStore, targetName);
    Path sqlFile = sqlDir.resolve(sqlValue);
    DataPath dataPath = DataPaths.ofQuery(sourceJdbcDataStore, Fs.getFileContent(sqlFile));
    Tabulars.transfer(dataPath, targetDataPath);
  }

  public static DbPlay of() {
    return new DbPlay();
  }

  public DbPlay setPlayFile(Path playFile) {
    this.playFile = playFile;
    return this;
  }

  public DbPlay setHomDirectory(Path homeDirectory) {
    this.homeDirectory = homeDirectory;
    return this;
  }

  public Path getDataDir() {
    return homeDirectory.resolve("data");
  }


}
