package net.bytle.db.jdbc;

import net.bytle.db.database.DataStore;

/**
 * An object with all meta information about a JDBC data store
 *
 */
public class Database extends DataStore {


  public static final String JDBC_SCHEME = "jdbc";

  // Jdbc
  public static final String DRIVER_PROPERTY_KEY = "driver";
  public static final String POST_STATEMENT_PROPERTY_KEY = "post_statement";

  Database(String name) {
    super(name);
  }

  public Database(DataStore dataStore) {
    super(dataStore);
  }

  public static Database of(String name) {
    return new Database(name);
  }

  public static Database of(DataStore dataStore) {
    return new Database(dataStore);
  }


  /**
   * This is a JDBC connection parameter
   * It should be threated as {@link #addProperty(String, String)}
   *
   * @param jdbcDriver
   * @return
   */
  public Database setDriver(String jdbcDriver) {
    super.addProperty(DRIVER_PROPERTY_KEY, jdbcDriver);
    return this;
  }



  public Database setPostConnectionStatement(String connectionScriptValue) {
    super.addProperty(POST_STATEMENT_PROPERTY_KEY,connectionScriptValue);
    return this;
  }


  public String getDriver() {

    return super.getProperty(DRIVER_PROPERTY_KEY);

  }

  public String getPostConnectionStatement() {

    return super.getProperty(POST_STATEMENT_PROPERTY_KEY);

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
