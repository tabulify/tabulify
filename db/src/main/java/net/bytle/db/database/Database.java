package net.bytle.db.database;

import net.bytle.db.spi.DataPath;

/**
 * An object with all meta information about a data store
 * in order to create connections
 */
public class Database extends DataStore  {


  // Jdbc
  private String driver;
  private String postStatement;

  Database(String name) {

    super(name);

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



  public Database setStatement(String connectionScriptValue) {
    this.postStatement = connectionScriptValue;
    return this;
  }


  public String getDriver() {
    return this.driver;
  }

  public String getConnectionStatement() {
    return this.postStatement;
  }


  /**
   * @param query          - the query
   * @return a data path query
   */
  public DataPath getQueryDataPath(String query) {

    return  getTableSystem().getProcessingEngine().getQuery(query);

  }

  @Override
  public Database setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  @Override
  public Database setUser(String user) {
    super.setUser(user);
    return this;
  }

  @Override
  public Database setPassword(String password) {
    super.setPassword(password);
    return this;
  }
}
