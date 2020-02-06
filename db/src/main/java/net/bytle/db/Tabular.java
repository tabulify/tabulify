package net.bytle.db;

import net.bytle.db.database.DataStore;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.Database;
import net.bytle.db.database.FileDataStore;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.engine.Queries;
import net.bytle.db.memory.MemorySystemProvider;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.type.Maps;
import net.bytle.type.Strings;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tabular is a global domain.
 * It's the entry point of every tabular/data application
 * It has knowledge of:
 * * the {@link DatastoreVault}
 * * and of the {@link net.bytle.db.database.Database}
 * and therefore is the main entry to create a data path from an URI
 * * a datastore object
 * * a connection
 * * or
 */
public class Tabular implements AutoCloseable {


  // Named used to create default path
  public static final String APP_NAME = "BytleDb";

  /**
   * The local file database name as also stated
   * by the {@link Path#getFileSystem()}
   */
  public static final String LOCAL_FILE_SCHEME = "file";
  public static final String MEMORY_DATASTORE = "memory";
  public static final String TPCDS_DATASTORE = "tpcds";


  /**
   * The memory database
   */
  public static final String DEFAULT_DATASTORE = MEMORY_DATASTORE;


  DatastoreVault dataStoreVault = null;

  Map<String, DataStore> dataStores = new HashMap<>();
  private String passphrase;


  public Tabular() {

    // Intern datastore

    // Local file System
    FileDataStore fileDataStore = FileDataStore.LOCAL_FILE_STORE;
    dataStores.put(fileDataStore.getName(), fileDataStore);

    // Memory
    Database memoryDataBase = Database.of(MEMORY_DATASTORE)
      .setConnectionString(MemorySystemProvider.SCHEME);
    dataStores.put(memoryDataBase.getName(), memoryDataBase);


    DataStore tpcDs = Database.of(TPCDS_DATASTORE)
      .setConnectionString(TPCDS_DATASTORE)
      .addProperty("scale", "0.01");
    dataStores.put(tpcDs.getName(), tpcDs);

  }

  public static Tabular tabular() {
    return new Tabular();
  }


  public List<DataStore> getDataStores() {
    return new ArrayList<>(dataStores.values());
  }

  /**
   * @param dataUri - A data uri defining the first data path
   * @param parts   - The child of the first one if any
   * @return
   */
  public DataPath getDataPath(String dataUri, String... parts) {

    // First argument
    if (!dataUri.contains(DataUri.AT_STRING)) {
      dataUri = dataUri + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }
    DataUri dataUriObj = DataUri.of(dataUri);
    String dataStoreName = dataUriObj.getDataStore();
    DataPath dataUriPath = getDataStore(dataStoreName)
      .getDataPath(dataUriObj.getPath());

    // Second argument to get the childs
    for (int i = 0; i < parts.length; i++) {
      dataUriPath = dataUriPath.getChild(parts[i]);
    }
    return dataUriPath;

  }

  /**
   * @param dataStoreName
   * @return a datastore
   * <p>
   * There is two specials data store always available (namely file and memory that store data respectively on the local file system and in memory)
   */
  public DataStore getDataStore(String dataStoreName) {

    return dataStores.get(dataStoreName);

  }

  public DatastoreVault getDataStoreVault() {
    return this.dataStoreVault;
  }

  public Tabular setDataStoreVault(Path storagePath) {
    dataStoreVault = DatastoreVault.of(storagePath);
    if (passphrase != null) {
      dataStoreVault.setPassphrase(passphrase);
    }
    dataStoreVault.getDataStores().forEach(
      ds -> {
        dataStores.put(ds.getName(), ds);
      }
    );
    return this;
  }

  public DataStore getOrCreateDataStore(String dataStoreName) {
    DataStore dataStore = dataStores.get(dataStoreName);
    if (dataStore == null) {
      dataStore = DataStore.of(dataStoreName);
      dataStores.put(dataStore.getName(), dataStore);
    }
    return dataStore;
  }


  public void close() {
    for (DataStore dataStore : dataStores.values()) {
      try {
        // A data store that was not used will have no data system
        if (dataStore.isOpen()) {
          dataStore.close();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public DataPath getDataPath(Path path) {

    String databaseName = FileDataStore.getDataStoreName(path.toUri());
    FileDataStore dataStore = (FileDataStore) this.dataStores.get(databaseName);
    if (dataStore == null) {
      dataStore = FileDataStore.of(path);
      this.dataStores.put(dataStore.getName(), dataStore);
    }
    return dataStore.getDataPath(path);

  }

  /**
   * Use the default storage location
   *
   * @return
   */
  public Tabular withDefaultStorage() {
    setDataStoreVault(DatastoreVault.DEFAULT_STORAGE_FILE);
    return this;
  }

  public Tabular setPassphrase(String passphrase) {
    this.passphrase = passphrase;
    if (dataStoreVault != null) {
      dataStoreVault.setPassphrase(passphrase);
    }
    return this;
  }


  /**
   * An utility function that returns data paths selected from a data uri pattern (ie glob_pattern@datastore)
   *
   * This function takes into account:
   *   * the query uri pattern (ie queryPattern*.sql@datastore, `select 1@datastore`)
   *   * and a entity uri pattern (ie table*@datastore)
   *
   * @param dataUriPattern
   * @return the data paths that the data uri pattern selects
   */
  public List<DataPath> select(String dataUriPattern) {
    List<DataPath> dataPathsToReturn = new ArrayList<>();
    if (!dataUriPattern.contains(DataUri.AT_STRING)) {
      dataUriPattern = dataUriPattern + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }
    DataUri dataUri = DataUri.of(dataUriPattern);
    String pathInUri = dataUri.getPath();

    // Query URI with an inline query ?
    if (Queries.isQuery(pathInUri)) {
      DataPath inlineQueryDataPath = this
        .getDataStore(dataUri.getDataStore())
        .getQueryDataPath(pathInUri)
        .setDescription("Inline Query");
      dataPathsToReturn.add(inlineQueryDataPath);
    } else {
      // Query URI with a path that defines Sql File ?
      if (Fs.getExtension(pathInUri).equals("sql")) {
        List<Path> files = Fs.getFilesByGlob(pathInUri);
        if (files.size() == 0) {
          return dataPathsToReturn;
        }
        for (Path path : files) {
          String query = Fs.getFileContent(path);
          if (Queries.isQuery(query)) {
            String queryName = Fs.getFileNameWithoutExtension(path);
            DataPath queryDataPath = this
              .getDataStore(dataUri.getDataStore())
              .getQueryDataPath(query)
              .setDescription(queryName);
            dataPathsToReturn.add(queryDataPath);
          } else {
            String msg = Strings.multiline("The query uri pattern (" + dataUriPattern + ") has selected the file (" + path + ") that seems to not contain a query",
              "The file content is: ",
              Strings.toStringNullSafe(query)
            );
            throw new RuntimeException(msg);
          }
        }
      } else {
        // Normal data uri pattern
        String dataStoreName = dataUri.getDataStore();
        DataPath currentDataPath = getDataStore(dataStoreName)
          .getCurrentDataPath();
        String glob = dataUri.getPath();
        dataPathsToReturn = currentDataPath.getDataSystem().getDescendants(currentDataPath, glob);
      }
    }
    return dataPathsToReturn;
  }

  /**
   *
   * Return a data path from a data def file.
   * The name of the data path is the file name without the {@link TableDef#DATA_DEF_EXTENSION data def extension}
   *
   * @param dataDefPath
   * @return the data path with its meta
   */
  public DataPath getDataPathOfDataDef(Path dataDefPath) {

    assert Files.exists(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath().toString() + " does not exist";
    assert Files.isRegularFile(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath().toString() + " does not exist";
    assert dataDefPath.getFileName().toString().contains(TableDef.DATA_DEF_EXTENSION): "The file ("+dataDefPath.getFileName().toString()+") has not the data def extension ("+TableDef.DATA_DEF_EXTENSION+")";

    String name = dataDefPath.getFileName().toString().replace(TableDef.DATA_DEF_EXTENSION, "");
    DataPath dataPath = this.getDataPath(name);

    InputStream input;
    try {
      input = Files.newInputStream(dataDefPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Transform the file in properties
    Yaml yaml = new Yaml();

    // Every document is one dataDef
    List<Map<String, Object>> documents = new ArrayList<>();
    for (Object data : yaml.loadAll(input)) {
      Map<String, Object> document;
      try {
        document = (Map<String, Object>) data;
      } catch (ClassCastException e) {
        String message = "A data Def must be in a map format. ";
        if (data.getClass().equals(java.util.ArrayList.class)) {
          message += "They are in a list format. You should suppress the minus if they are present.";
        }
        message += "The Bad Data Def Values are: " + data;
        throw new RuntimeException(message, e);
      }
      documents.add(document);
    }

    switch (documents.size()) {
      case 0:
        break;
      case 1:

        Map<String, Object> document = documents.get(0);

        // Loop through all other properties
        for (Map.Entry<String, Object> entry : document.entrySet()) {

          switch (entry.getKey().toLowerCase()) {
            case "name":
              continue;
            case "columns":
              Map<String, Object> columns;
              try {
                columns = (Map<String, Object>) entry.getValue();
              } catch (ClassCastException e) {
                String message = "The columns of the data def file (" + dataDefPath.toString() + ") must be in a map format. ";
                if (entry.getValue().getClass().equals(java.util.ArrayList.class)) {
                  message += "They are in a list format. You should suppress the minus if they are present.";
                }
                message += "Bad Columns Values are: " + entry.getValue();
                throw new RuntimeException(message, e);
              }
              for (Map.Entry<String, Object> column : columns.entrySet()) {

                try {
                  Map<String, Object> columnProperties = (Map<String, Object>) column.getValue();

                  String type = "varchar";
                  Object oType = Maps.getPropertyCaseIndependent(columnProperties, "type");
                  if (oType != null) {
                    type = (String) oType;
                  }

                  DataTypeJdbc dataTypeJdbc = DataTypesJdbc.of(type);

                  ColumnDef columnDef = dataPath.getDataDef().getColumnOf(column.getKey(), dataTypeJdbc.getClass());
                  for (Map.Entry<String, Object> columnProperty : columnProperties.entrySet()) {
                    switch (columnProperty.getKey().toLowerCase()) {
                      case "type":
                        columnDef.typeCode(dataTypeJdbc.getTypeCode());
                        break;
                      case "precision":
                        columnDef.precision((Integer) columnProperty.getValue());
                        break;
                      case "scale":
                        columnDef.scale((Integer) columnProperty.getValue());
                        break;
                      case "comment":
                        columnDef.comment((String) columnProperty.getValue());
                        break;
                      case "nullable":
                        columnDef.setNullable(Boolean.valueOf((String) columnProperty.getValue()));
                        break;
                      default:
                        columnDef.addProperty(columnProperty.getKey(), columnProperty.getValue());
                        break;
                    }
                  }

                } catch (ClassCastException e) {
                  String message = "The properties of column (" + column.getKey() + ") from the data def (" + dataPath.toString() + ") must be in a map format. ";
                  if (column.getValue().getClass().equals(java.util.ArrayList.class)) {
                    message += "They are in a list format. You should suppress the minus if they are present.";
                  }
                  message += "Bad Columns Properties Values are: " + column.getValue();
                  throw new RuntimeException(message, e);
                }
              }
              break;
            default:
              dataPath.getDataDef().addProperty(entry.getKey().toLowerCase(), entry.getValue());
              break;
          }
        }
        break;
      default:
        throw new RuntimeException("Too much metadata documents (" + documents.size() + ") found in the file (" + dataDefPath.toString() + ") for the dataPath (" + dataPath.toString() + ")");
    }
    return dataPath;

  }
}
