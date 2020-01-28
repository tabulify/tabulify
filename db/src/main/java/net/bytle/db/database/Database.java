package net.bytle.db.database;

import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.DataSetSystem;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An object with all meta information about a data store
 * in order to create connections
 */
public class Database implements Comparable<Database> {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


  // The database name
  private final String name;

  // The databaseStore from the database (if any))
  private DatabasesStore databaseStore;

  // Connection Url
  private String connectionString;

  // Jdbc
  private String driver;
  private String postStatement;

  // Authority
  private String user;
  private String password;

  // Path
  private String workingPath;

  // Env (equivalent Url query)
  Map<String, String> properties = new HashMap<>();

  // The equivalent of a connection with actions implementation
  private DataSetSystem dataSetSystem;

  Database(String name) {

    this.name = name;

  }

  public static Database of(String name) {
    return new Database(name);
  }


  /**
   * This is a JDBC connection parameter
   * It should be threated as {@link #addProperty(String, String)}
   *
   * @param jdbcDriver
   * @return
   */
  public Database setDriver(String jdbcDriver) {
    this.driver = jdbcDriver;
    return this;
  }


  public String getDatabaseName() {
    return name;
  }

  public String getConnectionString() {
    return this.connectionString;
  }

  public Database setConnectionString(String connectionString) {

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
    Database database = (Database) o;
    return Objects.equals(name, database.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }


  public Database setUser(String user) {
    this.user = user;
    return this;
  }

  public Database setPassword(String pwd) {
    this.password = pwd;
    return this;
  }

  public Database setStatement(String connectionScriptValue) {
    this.postStatement = connectionScriptValue;
    return this;
  }


  public String getDriver() {
    return this.driver;
  }

  public String getUser() {
    return this.user;
  }

  public String getPassword() {
    return this.password;
  }

  public String getConnectionStatement() {
    return this.postStatement;
  }


  @Override
  public int compareTo(Database o) {

    return this.getDatabaseName().compareTo(o.getDatabaseName());

  }

  public Database setDatabaseStore(DatabasesStore databasesStore) {
    this.databaseStore = databasesStore;
    return this;
  }

  public DatabasesStore getDatabaseStore() {
    return this.databaseStore;
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

  public Database setWorkingPath(String path) {
    this.workingPath = path;
    return this;
  }

  public Database addProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  public URI getUri() {

    switch (getScheme()) {
      case "jdbc":
        throw new RuntimeException("Jdbc connection string cannot be casted to a URI");
      default:
        return URI.create(connectionString);
    }

  }


  /**
   * @param path
   * @return a data path from the current database and its path
   */
  public DataPath getDataPath(String path) {
    DataUri dataUri = DataUri.of(path + DataUri.AT_STRING + this.getDatabaseName());
    return DataPaths.of(this, dataUri);
  }

  public DataPath getCurrentDataPath() {
    return DataPaths.of(this, ".");
  }

  public DataSetSystem getDataSystem() {
    return dataSetSystem;
  }
}
