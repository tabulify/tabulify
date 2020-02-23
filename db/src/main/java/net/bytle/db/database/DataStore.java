package net.bytle.db.database;

import net.bytle.db.DatastoreVault;
import net.bytle.db.DbLoggers;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.*;
import net.bytle.db.uri.Uris;
import net.bytle.type.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class DataStore implements Comparable<DataStore>, AutoCloseable {

  private final static Logger logger = LoggerFactory.getLogger(DataStore.class);

  // The database name
  private final String name;

  // Connection Url
  protected String connectionString;
  private String description;

  // An object that have all sql data type function
  private SqlDataTypesManager sqlDataTypeManager;

  // The run is strict
  private boolean strict = true;


  public DataStore(String name, String connectionString) {
    this.name = name;
    this.connectionString = connectionString;
  }

  // Env (equivalent Url query)
  Map<String, String> properties = new HashMap<>();


  // Authority
  private String user;
  private String password;

  /**
   * Deep copy
   *
   * @param ds
   * @return a new reference
   * Used in the {@link DatastoreVault#load() datastore vault load function} to create a deep copy of the
   * internal data stores.
   */
  public static DataStore of(DataStore ds) {
    return DataStore.of(ds.getName(), ds.getConnectionString())
      .setPassword(ds.getPassword())
      .setUser(ds.getUser())
      .setProperties(ds.getProperties())
      .setDescription(ds.getDescription());
  }

  public static FsDataStore of(Path path) {
    FsDataStore fsDataStore;
    if (path.toUri().getScheme().equals("file")){
      fsDataStore = FsDataStore.getLocalFileSystem();
    } else {
      FileSystem fileSystem = path.getFileSystem();
      fsDataStore = new FsDataStore(fileSystem.toString(), path.toUri().toString(), fileSystem);
    }
    return fsDataStore;
  }

  public String getDescription() {
    return description;
  }


  public String getName() {
    return name;
  }

  public String getConnectionString() {
    return this.connectionString;
  }


  public DataStore setConnectionString(String connectionString) {
    assert connectionString != null : "A connection string cannot be null (for the data store " + this.name + ")";

    this.connectionString = connectionString;
    return this;

  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataStore dataStore = (DataStore) o;
    return Objects.equals(name, dataStore.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public DataStore setUser(String user) {
    this.user = user;
    return this;
  }

  public DataStore setPassword(String pwd) {
    this.password = pwd;
    return this;
  }

  public String getUser() {
    return this.user;
  }

  public String getPassword() {
    return this.password;
  }

  @Override
  public int compareTo(DataStore o) {

    return this.getName().compareTo(o.getName());

  }

  /**
   * @return the scheme of the data store
   * * file
   * * ...
   */
  public String getScheme() {
    return connectionString != null ? Uris.getScheme(connectionString) : "";
  }


  public DataStore addProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  public static DataStore of(String name, String url) {

    String scheme = Uris.getScheme(url);
    TableSystem tableSystem = createTableSystem(scheme);
    if (tableSystem == null) {
      final String message = "No provider was found for the scheme (" + scheme + ") from the dataStore (" + name + ") with the Url (" + url + ")";
      DbLoggers.LOGGER_DB_ENGINE.warning(message);
      return new DataStoreWithoutProvider(name, url);
    } else {
      return tableSystem.createDataStore(name, url);
    }

  }

  public abstract TableSystem getDataSystem();


  static protected TableSystem createTableSystem(String scheme) {

    TableSystem tableSystem = null;
    List<TableSystemProvider> installedProviders = TableSystemProvider.installedProviders();
    for (TableSystemProvider tableSystemProvider : installedProviders) {
      if (tableSystemProvider.getSchemes().contains(scheme)) {
        tableSystem = tableSystemProvider.getTableSystem();
        if (tableSystem == null) {
          String message = "The table system is null for the provider (" + tableSystemProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
      }
    }

    return tableSystem;
  }

  /**
   * @param parts
   * @return a data path from the current database and its path
   */
  public abstract DataPath getDataPath(String... parts);


  /**
   * @return the current/working path of this data store
   */
  public abstract DataPath getCurrentDataPath();

  @Override
  public void close() {
    // Nothing to do here
  }

  public Double getPropertyAsDouble(String property) {
    String value = properties.get(property);
    if (value == null) {
      return null;
    } else {
      try {
        return Double.valueOf(value);
      } catch (Exception e) {
        logger.error("The value for the property (" + property + ") of the data store (" + this.getName() + ") is not in a double format (" + value + ")");
        throw new RuntimeException(e);
      }
    }
  }


  public String getProperty(String propertyKey) {
    return properties.get(propertyKey);
  }

  public Map<String, String> getProperties() {
    return this.properties;
  }

  protected DataStore setProperties(Map<String, String> properties) {
    this.properties = properties;
    return this;
  }

  /**
   * Return true if this data store is open
   * <p>
   * This is to prevent a close error when a data store is:
   * * not used
   * * in the list
   * * its data system is not in the classpath
   * <p>
   * Example: the file datastore will have no provider when developing the db-jdbc module
   *
   * @return true if this data system was build
   */
  public boolean isOpen() {
    return false;
  }

  /**
   * @param query - the query
   * @return a data path query
   */
  public abstract DataPath getQueryDataPath(String query);

  /**
   * @param dataDefPath - the path of a data def file
   * @return a data path from a data def path
   * It will create or merge the data path from the data def file
   * <p>
   * If the data document already exist in the data store, it will merge, otherwise it will create it.
   */
  public DataPath createOrMergeDataPathOfDataDef(Path dataDefPath) {
    assert Files.exists(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath().toString() + " does not exist";
    assert Files.isRegularFile(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath().toString() + " does not exist";
    assert dataDefPath.getFileName().toString().contains(TableDef.DATA_DEF_SUFFIX) : "The file (" + dataDefPath.getFileName().toString() + ") has not the data def extension (" + TableDef.DATA_DEF_SUFFIX + ")";

    String name = dataDefPath.getFileName().toString().replace(TableDef.DATA_DEF_SUFFIX, "");
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

                  ColumnDef columnDef = dataPath.getDataDef().getColumnDef(column.getKey());
                  // If the columns does not exist
                  if (columnDef == null) {
                    String type = "varchar";
                    Object oType = Maps.getPropertyCaseIndependent(columnProperties, "type");
                    if (oType != null) {
                      type = (String) oType;
                    }
                    SqlDataType sqlDataType = getSqlDataTypeManager().get(type);
                    columnDef = dataPath.getDataDef().getColumnOf(column.getKey(), sqlDataType.getClazz());
                  }

                  for (Map.Entry<String, Object> columnProperty : columnProperties.entrySet()) {
                    switch (columnProperty.getKey().toLowerCase()) {
                      case "type":
                        // Already done during the creation
                        break;
                      case "precision":
                        if (columnDef.getPrecision() == null) {
                          columnDef.precision((Integer) columnProperty.getValue());
                        }
                        break;
                      case "scale":
                        if (columnDef.getScale() == null) {
                          columnDef.scale((Integer) columnProperty.getValue());
                        }
                        break;
                      case "comment":
                        if (columnDef.getComment() == null) {
                          columnDef.comment((String) columnProperty.getValue());
                        }
                        break;
                      case "nullable":
                        if (columnDef.getNullable() == null) {
                          columnDef.setNullable(Boolean.valueOf((String) columnProperty.getValue()));
                        }
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


  public DataStore setDescription(String description) {
    this.description = description;
    return this;
  }

  public List<DataPath> select(String pattern) {
    return getDataSystem().getDescendants(getCurrentDataPath(), pattern);
  }


  /**
   * @return The number of thread that can be created against the data store
   */
  public abstract Integer getMaxWriterConnection();

  /**
   * An init function
   *
   * @return
   */
  private SqlDataTypesManager getSqlDataTypeManager() {
    if (sqlDataTypeManager == null) {
      sqlDataTypeManager = new SqlDataTypesManager();
    }
    return sqlDataTypeManager;
  }

  /**
   * @return all data types
   */
  public Set<SqlDataType> getSqlDataTypes() {

    return getSqlDataTypeManager().getDataTypes();
  }

  /**
   * @param typeCode
   * @return the data type for one type
   */
  public SqlDataType getSqlDataType(Integer typeCode) {
    return getSqlDataTypeManager().get(typeCode);
  }

  /**
   * @param typeName
   * @return the data type for one name
   */
  public SqlDataType getSqlDataType(String typeName) {
    return getSqlDataTypeManager().get(typeName);
  }

  /**
   * @param clazz
   * @return @return the sql data type for a java class
   */
  public SqlDataType getSqlDataType(Class<?> clazz) {
    return getSqlDataTypeManager().ofClass(clazz);
  }

  public abstract ProcessingEngine getProcessingEngine();


  public void addSqlDataType(SqlDataType sqlDataType) {
    getSqlDataTypeManager().addSqlDataType(sqlDataType);
  }

  public DataStore setStrict(boolean strict) {
    this.strict = strict;
    return this;
  }

  public boolean isStrict() {
    return strict;
  }


}
