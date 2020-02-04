package net.bytle.db.database;

import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.uri.Uris;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataStore implements Comparable<DataStore>, AutoCloseable {

  private final static Logger logger = LoggerFactory.getLogger(DataStore.class);

  // The database name
  private final String name;

  // Connection Url
  protected String connectionString;


  protected DataStore(String name) {
    this.name = name;
  }

  // Path
  private String workingPath;

  // Env (equivalent Url query)
  Map<String, String> properties = new HashMap<>();

  // The equivalent of a connection with actions implementation
  private TableSystem tableSystem;

  // Authority
  private String user;
  private String password;

  // Creating a data store from a data store
  public DataStore(DataStore dataStore) {
    this.name = dataStore.getName();
    this.setUser(dataStore.getUser());
    this.setPassword(dataStore.getPassword());
    this.setConnectionString(dataStore.getConnectionString());
    this.setProperties(dataStore.getProperties());
  }

  public String getName() {
    return name;
  }

  public String getConnectionString() {
    return this.connectionString;
  }


  public DataStore setConnectionString(String connectionString) {
    assert connectionString != null : "A connection string cannot be null (for the data store "+this.name+")";

    if (this.connectionString == null || this.connectionString.equals(connectionString)) {

      this.connectionString = connectionString;
      return this;

    } else {

      throw new RuntimeException("The connection string cannot be changed. It has already the value (" + this.connectionString + ") and cannot be set to (" + connectionString + ")");

    }


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

  public DataStore setWorkingPath(String path) {
    this.workingPath = path;
    return this;
  }

  public DataStore addProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  public static DataStore of(String name) {
    return new DataStore(name);
  }

  public TableSystem getDataSystem() {
    if (tableSystem == null) {
      tableSystem = getTableSystem();
    }
    return tableSystem;
  }

  protected TableSystem getTableSystem() {
    if (tableSystem == null) {
      List<TableSystemProvider> installedProviders = TableSystemProvider.installedProviders();
      String scheme = this.getScheme();
      for (TableSystemProvider tableSystemProvider : installedProviders) {
        if (tableSystemProvider.getSchemes().contains(scheme)) {
          tableSystem = tableSystemProvider.getTableSystem(this);
          if (tableSystem == null) {
            String message = "The table system is null for the provider (" + tableSystemProvider.getClass().toString() + ")";
            DbLoggers.LOGGER_DB_ENGINE.severe(message);
            throw new RuntimeException(message);
          }
        }
      }
      if (tableSystem == null) {
        final String message = "No provider was found for the scheme (" + scheme + ") from the dataStore (" + this.getName() + ") with the Url (" + this.getConnectionString() + ")";
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new RuntimeException(message);
      }
    }
    return tableSystem;
  }

  /**
   * @param parts
   * @return a data path from the current database and its path
   */
  public DataPath getDataPath(String... parts) {

    return getDataSystem().getDataPath(parts);

  }


  /**
   * @return the current/working path of this data store
   */
  public DataPath getCurrentDataPath() {
    return getDataSystem().getCurrentPath();
  }

  @Override
  public void close() {
    try {
      tableSystem.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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


  protected String getProperty(String propertyKey) {
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
   *
   * This is to prevent a close error when a data store is:
   *   * not used
   *   * in the list
   *   * its data system is not in the classpath
   *
   * Example: the file datastore will have no provider when developing the db-jdbc module
   *
   * @return true if this data system was build
   */
  public boolean isOpen() {
    return tableSystem !=null;
  }

  /**
   * @param query - the query
   * @return a data path query
   */
  public DataPath getQueryDataPath(String query) {

    return  getTableSystem().getProcessingEngine().getQuery(query);

  }

}
