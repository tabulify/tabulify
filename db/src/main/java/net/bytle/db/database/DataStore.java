package net.bytle.db.database;

import net.bytle.db.DatastoreVault;
import net.bytle.db.DbLoggers;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.DataStoreProvider;
import net.bytle.db.uri.Uris;
import net.bytle.type.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A data store
 */
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
    return DataStore.createDataStoreFromProviderOrDefault(ds.getName(), ds.getConnectionString())
      .setPassword(ds.getPassword())
      .setUser(ds.getUser())
      .setProperties(ds.getProperties())
      .setDescription(ds.getDescription());
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

  public static DataStore createDataStoreFromProviderOrDefault(String name, String url) {

    List<DataStoreProvider> installedProviders = DataStoreProvider.installedProviders();
    for (DataStoreProvider dataStoreProvider : installedProviders) {
      if (dataStoreProvider.accept(url)) {
        DataStore dataStore = dataStoreProvider.createDataStore(name, url);
        if (dataStore == null) {
          String message = "The table system is null for the provider (" + dataStoreProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
        return dataStore;
      }
    }

    // No provider was found
    final String message = "No provider was found for the url (" + url + ") from the dataStore (" + name + ") with the Url (" + url + ")";
    DbLoggers.LOGGER_DB_ENGINE.warning(message);
    return new DataStoreWithoutProvider(name, url);

  }

  public abstract DataSystem getDataSystem();


  /**
   * @param parts
   * @return a data path with the default type from the data store
   * To get a data path from a specific type, use the function {@link #getTypedDataPath(String, String...)}
   */
  public abstract DataPath getDefaultDataPath(String... parts);

  /**
   *
   * @param type - the {@link DataPath#getType()} of a data path
   * @param parts - the parts of the data path
   * @return a data path with the designed type from the data store
   */
  public abstract DataPath getTypedDataPath(String type, String... parts);


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

  public DataStore setProperties(Map<String, String> properties) {
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
   *
   * @param dataDefPath
   * @return create a data path
   */
  public DataPath getDataPathOfDataDefAndMerge(Path dataDefPath) {
    String fileName = dataDefPath.getFileName().toString();
    String name = fileName.substring(0,fileName.lastIndexOf("--"));
    DataPath dataPath = this.getDefaultDataPath(name);
    mergeDataDefFromFile(dataPath.getOrCreateDataDef(), dataDefPath);
    return dataPath;
  }
  /**
   * @param dataDefPath - the path of a data def file
   * @return a data path from a data def path
   * It will create or merge the data path from the data def file
   * <p>
   * If the data document already exist in the data store, it will merge, otherwise it will create it.
   */
  public void mergeDataDefFromFile(RelationDef dataDef, Path dataDefPath) {
    assert Files.exists(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath().toString() + " does not exist";
    assert Files.isRegularFile(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath().toString() + " does not exist";
    String fileName = dataDefPath.getFileName().toString();
    assert fileName.matches("(.*)--(.*).yml") : "The file (" + fileName + ") has not the data def extension (" + TableDef.DATA_DEF_SUFFIX + ")";

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

                  ColumnDef columnDef = dataDef.getColumnDef(column.getKey());
                  // If the columns does not exist
                  if (columnDef == null) {
                    String type = "varchar";
                    Object oType = Maps.getPropertyCaseIndependent(columnProperties, "type");
                    if (oType != null) {
                      type = (String) oType;
                    }
                    SqlDataType sqlDataType = getSqlDataTypeManager().get(type);
                    columnDef = dataDef.getOrCreateColumn(column.getKey(), sqlDataType.getClazz());
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
                  String message = "The properties of column (" + column.getKey() + ") from the data def (" + dataDef.toString() + ") must be in a map format. ";
                  if (column.getValue().getClass().equals(java.util.ArrayList.class)) {
                    message += "They are in a list format. You should suppress the minus if they are present.";
                  }
                  message += "Bad Columns Properties Values are: " + column.getValue();
                  throw new RuntimeException(message, e);
                }
              }
              break;
            default:
              dataDef.addProperty(entry.getKey().toLowerCase(), entry.getValue());
              break;
          }
        }
        break;
      default:
        throw new RuntimeException("Too much metadata documents (" + documents.size() + ") found in the file (" + dataDefPath.toString() + ") for the dataPath (" + dataDef.toString() + ")");
    }
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

  /**
   * When running in a non-strict mode,
   * the process will try to correct common error/warning
   *
   * Example:
   *   * A field in a CSV file that is wrapped in double quote will be unwrapped even if the double quote is not set in the structure
   *
   * @param strict
   * @return
   */
  public DataStore setStrict(boolean strict) {
    this.strict = strict;
    return this;
  }

  public boolean isStrict() {
    return strict;
  }


  /**
   * @return a default typed data path with a UUID v4 name
   */
  public DataPath getAndCreateRandomDataPath(){
    return getDefaultDataPath(UUID.randomUUID().toString());
  }

  /**
   * Transform an object into the desired clazz
   * This function takes over if the result set function with the class attribute
   * such as {@link java.sql.ResultSet#getObject(String, Class)} are not supported
   * @param object
   * @param clazz
   * @param <T>
   * @return
   */
  public abstract <T> T getObject(Object object, Class<T> clazz);

}
