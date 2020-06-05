package net.bytle.db.play;

import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.database.DataStore;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.jdbc.SqlDataPath;
import net.bytle.db.jdbc.SqlDataStore;
import net.bytle.db.spi.DataPath;
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

  /**
   * The path of the play file
   */
  private Path playFile;

  /**
   * The path of the project (called also root or project directory)
   */
  private Path homeDirectory;

  /**
   * The base path for all sql query
   */
  private Path sqlDir;

  /**
   * The tabular (ie the environment that will run the play)
   */
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

  /**
   * A load
   * @param task
   */
  private void loadTask(Map<String, Object> task) {

    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the load operations: " + desc);


    // Source
    Object sourceValue = Maps.getPropertyCaseIndependent(task, "source");
    String source;
    if (sourceValue instanceof String) {
      source = (String) sourceValue;
    } else {
      throw new RuntimeException("The source is not a string but a "+sourceValue.getClass().toString());
    }

    // File in data dir
    Path dataDir = this.getDataDir();
    Path sourceFile = dataDir.resolve(source);
    DataPath sourceDataPath = null;
    if (Files.exists(sourceFile)){
      sourceDataPath = tabular.getDataPath(sourceFile);
    }
    if (sourceDataPath == null){
      throw new RuntimeException("Unable to locate the source");
    }

    // Csv
    if (sourceDataPath instanceof CsvDataPath){
      sourceDataPath = ((CsvDataPath) sourceDataPath)
        .getOrCreateDataDef()
        .setHeaderRowId(1)
        .getDataPath();
    }

    /**
     * Sql (Query, ...)
     */
    if (sourceDataPath instanceof SqlDataPath){
      Map<String, Object> targetValues = (Map<String, Object>) sourceValue;
      String dsn = (String) Maps.getPropertyCaseIndependent(targetValues, "dsn");
    }


    // Target
    Object targetValue = Maps.getPropertyCaseIndependent(task, "target");
    DataPath targetDataPath = null;
    DataStore jdbcDataStore;
    if (targetValue instanceof String) {
      // Uri
      DataUri datUri = DataUri.of((String) targetValue);
      jdbcDataStore = tabular.getDataStore(datUri.getDataStore());
      String path = datUri.getPath();
      if (path == null){
        path = sourceDataPath.getName();
      }
      targetDataPath = jdbcDataStore.getDefaultDataPath(path);
    } else {
      Map<String, Object> targetValues = (Map<String, Object>) targetValue;
      String dsn = (String) Maps.getPropertyCaseIndependent(targetValues, "dsn");
      String targetPathValue = (String) Maps.getPropertyCaseIndependent(targetValues, "name");
    }


    Tabulars.copy(sourceDataPath, targetDataPath);
    System.out.println("The load operations has terminated");
  }

  private void sqlTask(Map<String, Object> task) {

    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the sql operations: " + desc);
    String sourceValue = (String) Maps.getPropertyCaseIndependent(task, "source");
    Object targetValue = Maps.getPropertyCaseIndependent(task, "target");
    String targetDsn = null;
    String targetName = null;
    if (targetValue instanceof Map) {
      Map<String, Object> targetMap = (Map<String, Object>) targetValue;
      targetDsn = (String) Maps.getPropertyCaseIndependent(targetMap, "dsn");
      targetName = (String) Maps.getPropertyCaseIndependent(targetMap, "name");
    }
    SqlDataStore targetJdbcDataStore = (SqlDataStore) tabular.getDataStore(targetDsn);
    String sqlValue = (String) Maps.getPropertyCaseIndependent(task, "sql");
    if (targetName == null) {
      targetName = Fs.getFileNameWithoutExtension(sqlValue);
    }
    DataPath targetDataPath = targetJdbcDataStore.getDefaultDataPath( targetName);
    Path sqlFile = sqlDir.resolve(sqlValue);
    DataPath dataPath = targetJdbcDataStore.getQueryDataPath(Fs.getFileContent(sqlFile));
    Tabulars.copy(dataPath, targetDataPath);
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
