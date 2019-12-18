package net.bytle.db.database;

import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.log.Log;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An object with all meta information about a data store
 * An resource object has an URL
 * but a connection also
 */
public class Database implements Comparable<Database> {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


  // The database name
  private final String name;

  // Does this database comes from a database store
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
  Map<String, String> env = new HashMap<>();


  Database(String name) {

    this.name = name;

  }

  public static Database of(String name) {
    return new Database(name);
  }


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
      return DatabasesStore.DEFAULT_DATABASE;
    } else {
      return getConnectionString().substring(0, getConnectionString().indexOf(":"));
    }
  }

  public Database setWorkingPath(String path) {
    this.workingPath = path;
    return this;
  }

  public String getWorkingPath() {
    return workingPath;
  }

  public Database addEnvironments(Map<String, String> env) {
    env.putAll(env);
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


}
