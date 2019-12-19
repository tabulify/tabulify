package net.bytle.db.play;

import net.bytle.db.DatabasesStore;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Dag;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.fs.Fs;
import net.bytle.type.Maps;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DbPlay {

  private static Path sqlDir;
  private static Path playDir;
  static private Path dataDir;
  private static DatabasesStore databasesStore;


  public static void main(String[] args) throws IOException {

    Path workingPath = Paths.get(".");
    Path playFile = workingPath.resolve("play.yml");
    if (args.length > 0) {
      playFile = Paths.get(args[0]);
    } else {
      System.err.println("A play should be given as first parameters");
    }
    if (args.length > 1) {
      workingPath = Paths.get(args[1]);
    }
    if (!Files.exists(playFile)) {
      System.err.println("The play file (" + playFile.toAbsolutePath().toString() + ") does not exist");
      System.exit(1);
    }

    dataDir = workingPath.resolve("data");
    playDir = workingPath.resolve("play");
    sqlDir = workingPath.resolve("sql");
    Path dsnDir = workingPath.resolve("dsn");
    databasesStore = DatabasesStore.of(dsnDir.resolve("dsn.ini"));


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

  private static void cleanTask(Map<String, Object> task) {
    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the clean operations: " + desc);
    String target = (String) Maps.getPropertyCaseIndependent(task, "target");
    Database database = databasesStore.getDatabase(target);
    DataPath dataPath = DataPaths.of(database, ".");
    List<DataPath> children = DataPaths.getChildren(dataPath);
    Dag.get(children).getDropOrderedTables()
      .stream().forEach(
      s -> {
        Tabulars.drop(s);
      }
    );
    System.out.println("The clean operations has succeeded and has deleted the following tables " + children);
  }

  private static void loadTask(Map<String, Object> task) {
    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the load operations: " + desc);
    String sourcePath = (String) Maps.getPropertyCaseIndependent(task, "source");
    Path source = dataDir.resolve(sourcePath);
    DataPath sourceDataPath = DataPaths.of(source);
    if (sourceDataPath.getClass().equals(CsvDataPath.class)) {
      sourceDataPath = ((CsvDataPath) sourceDataPath)
        .getDataDef()
        .setHeaderRowCount(1)
        .getDataPath();
    }
    String targetValue = (String) Maps.getPropertyCaseIndependent(task, "target");
    Database database = databasesStore.getDatabase(targetValue);
    String sourceFileName = source.getFileName().toString();
    String targetTableName = Fs.getFileName(sourceFileName);
    DataPath targetDataPath = DataPaths.of(database, targetTableName);
    Tabulars.transfer(sourceDataPath, targetDataPath);
    System.out.println("The load operations has terminated");
  }

  private static void sqlTask(Map<String, Object> task) {
    String desc = (String) Maps.getPropertyCaseIndependent(task, "desc");
    System.out.println("Starting the sql operations: " + desc);
    String sourceValue = (String) Maps.getPropertyCaseIndependent(task, "source");
    DatabasesStore databasesStore = DatabasesStore.of(dataDir.resolve("dsn.ini"));
    Database sourceDatabase = databasesStore.getDatabase(sourceValue);
    Object targetValue = Maps.getPropertyCaseIndependent(task, "target");
    String targetDsn = null;
    String targetName = null;
    if (targetValue instanceof Map) {
      Map<String, Object> targetMap = (Map<String, Object>) targetValue;
      targetDsn = (String) Maps.getPropertyCaseIndependent(targetMap, "dsn");
      targetName = (String) Maps.getPropertyCaseIndependent(targetMap, "name");
    }
    Database targetDatabase = databasesStore.getDatabase(targetDsn);
    String sqlValue = (String) Maps.getPropertyCaseIndependent(task, "sql");
    if (targetName == null) {
      targetName = Fs.getFileName(sqlValue);
    }
    DataPath targetDataPath = DataPaths.of(targetDatabase, targetName);
    Path sqlFile = sqlDir.resolve(sqlValue);
    DataPath dataPath = DataPaths.ofQuery(sourceDatabase, Fs.getFileContent(sqlFile));
    Tabulars.transfer(dataPath, targetDataPath);
  }

}
