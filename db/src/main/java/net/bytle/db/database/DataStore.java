package net.bytle.db.database;

import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class DataStore implements Comparable<DataStore>, AutoCloseable {

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

  public String getName() {
    return name;
  }

  public String getConnectionString() {
    return this.connectionString;
  }


  public DataStore setConnectionString(String connectionString) {

    if (this.connectionString == null || this.connectionString.equals(connectionString)) {

      this.connectionString = connectionString;

    } else {

      throw new RuntimeException("The connection string cannot be changed. It has already the value (" + this.connectionString + ") and cannot be set to (" + connectionString + ")");

    }
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
    if (connectionString == null) {
      throw new RuntimeException("The connection string is null");
    } else {
      int endIndex = getConnectionString().indexOf(":");
      if (endIndex == -1) {
        return getConnectionString();
      } else {
        return getConnectionString().substring(0, endIndex);
      }
    }
  }

  public DataStore setWorkingPath(String path) {
    this.workingPath = path;
    return this;
  }

  public DataStore addProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  public TableSystem getDataSystem() {
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
      if (tableSystem==null) {
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

    return getTableSystem().getDataPath(parts);

  }


  /**
   * @return the current/working path of this data store
   */
  public DataPath getCurrentDataPath() {
    return getTableSystem().getCurrentPath();
  }

  @Override
  public void close() throws Exception {
    tableSystem.close();
  }

}
