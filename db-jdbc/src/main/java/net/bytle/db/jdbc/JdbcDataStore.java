package net.bytle.db.jdbc;

import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.TableSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * An object with all meta information about a JDBC data store
 */
public class JdbcDataStore extends DataStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDataStore.class);

  @Override
  public JdbcDataStore addProperty(String key, String value) {
    super.addProperty(key, value);
    return this;
  }


  private Connection connection;

  public static final String DB_ORACLE = "Oracle";
  public static final String DB_HANA = "HDB";
  public static final String DB_SQL_SERVER = "Microsoft SQL Server";
  public static final String DB_SQLITE = "SQLite";
  public static final String DB_HIVE = "Apache Hive";

  // Jdbc
  public static final String DRIVER_PROPERTY_KEY = "driver";
  public static final String POST_STATEMENT_PROPERTY_KEY = "post_statement";
  private final JdbcDataSystem jdbcDataSystem;
  private JdbcDataStoreExtension sqlDatabase;
  private JdbcDataProcessingEngine processingEngine;


  public JdbcDataStore(String name, String url, JdbcDataSystem jdbcDataSystem) {
    super(name, url);
    this.jdbcDataSystem = jdbcDataSystem;
  }

  public static JdbcDataStore of(String name, String url) {

    return new JdbcDataStore(name, url, JdbcDataSystemProvider.jdbcDataSystem);

  }


  /**
   * This is a JDBC connection parameter
   * It should be threated as {@link #addProperty(String, String)}
   *
   * @param jdbcDriver
   * @return
   */
  public JdbcDataStore setDriver(String jdbcDriver) {
    super.addProperty(DRIVER_PROPERTY_KEY, jdbcDriver);
    return this;
  }


  public JdbcDataStore setPostConnectionStatement(String connectionScriptValue) {
    super.addProperty(POST_STATEMENT_PROPERTY_KEY, connectionScriptValue);
    return this;
  }


  public String getDriver() {

    return super.getProperty(DRIVER_PROPERTY_KEY);

  }

  public String getPostConnectionStatement() {

    return super.getProperty(POST_STATEMENT_PROPERTY_KEY);

  }


  @Override
  public JdbcDataStore setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  @Override
  public JdbcDataStore setUser(String user) {
    super.setUser(user);
    return this;
  }

  @Override
  public JdbcDataStore setPassword(String password) {
    super.setPassword(password);
    return this;
  }

  @Override
  public TableSystem getDataSystem() {
    return jdbcDataSystem;
  }

  @Override
  public JdbcDataPath getDataPath(String... names) {
    return this.getCurrentDataPath().resolve(names);
  }

  @Override
  public JdbcDataPath getCurrentDataPath() {
    return JdbcDataPath.of(this, getCurrentCatalog(), getCurrentSchema(), null);
  }

  public String getProductName() {
    try {
      return getCurrentConnection().getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  public JdbcDataStoreExtension getExtension() {

    if (sqlDatabase==null) {
      // check installed providers
      for (JdbcDataStoreExtensionProvider provider : JdbcDataStoreExtensionProvider.installedProviders()) {
        if (this.getProductName().equalsIgnoreCase(provider.getProductName())) {
          sqlDatabase = provider.getJdbcDataStoreExtension(this);
        }
      }
    }

    return sqlDatabase;

  }




  @Override
  public void close() {
    if (this.connection != null) {
      try {
        this.connection.close();
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("The connection of the database (" + this.getName() + ") was closed.");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public DataPath getQueryDataPath(String query) {
    return JdbcDataPath.ofQuery(this, query);
  }

  /**
   * Return the current Connection
   *
   * @return the current connection or null (if no URL)
   * <p>
   * The current connection is the first connection created
   */
  public Connection getCurrentConnection() {

    String location = "initial";
    if (this.connection == null) {
      this.connection = getNewConnection(location);
    }
    try {
      if (this.connection.isClosed()) {

        // With the database id being the database name, this is not true anymore ?
        // throw new RuntimeException("The connection was closed ! We cannot reopen it otherwise the object id will not be the same anymore");
        JdbcDataSystemLog.LOGGER_DB_JDBC.severe("The database connection was closed ! We reopen it.");
        this.connection = getNewConnection(location);

      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return this.connection;

  }

  /**
   * synchronized because it's used within thread
   *
   * @return return a new connection object
   */
  public synchronized Connection getNewConnection(String purpose) {


    URIExtended uriExtended = new URIExtended(this.getConnectionString());
    String driver = uriExtended.getDriver();

    if (driver != null) {
      try {
        Class.forName(driver);
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Driver " + driver + " loaded");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("The driver Class (" + driver + ") for the database (" + this.getName() + ")  could not be loaded. An error occurs: " + e.getMessage() + ". May be that the driver is not on the path ?", e);
      }
    }


    Connection connection;
    JdbcDataSystemLog.LOGGER_DB_JDBC.info("Trying to connect to the connection (" + this.getConnectionString() + ")");
    try {

      // connection = DriverManager.getConnection(this.url, this.user, this.password);
      Properties connectionProperties = new Properties();
      // Sql Server
      // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
      //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
      connectionProperties.put("applicationName", Tabular.APP_NAME + " " + this.getName() + " " + purpose);
      if (this.getUser() != null) {
        connectionProperties.put("user", this.getUser());
        if (this.getPassword() != null) {
          connectionProperties.put("password", this.getPassword());
        }
      }
      connection = DriverManager.getConnection(this.getConnectionString(), connectionProperties);

    } catch (SQLException e) {
      String msg = "Unable to connect to the database (" + this.getName() + ")with the following URL (" + this.getConnectionString() + "). Error: " + e.getMessage();
      JdbcDataSystemLog.LOGGER_DB_JDBC.severe(msg);
      throw new RuntimeException(e);
    }


    // Post Connection statement (such as alter session set current_schema)
    if (this.getPostConnectionStatement() != null) {
      try (CallableStatement callableStatement = connection.prepareCall(this.getPostConnectionStatement())) {

        callableStatement.execute();
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Post statement connection executed ("+this.getPostConnectionStatement()+")");
      } catch (SQLException e) {
        throw new RuntimeException("Post Connection error occurs: " + e.getMessage(), e);
      }
    }
    JdbcDataSystemLog.LOGGER_DB_JDBC.info("Connected !");
    return connection;

  }

  /**
   * @return the number of concurrent writer connection
   */
  public Integer getMaxWriterConnection() {
    Integer maxWriterConnection = null;
    if (this.getExtension() != null) {
      maxWriterConnection = this.getExtension().getMaxWriterConnection();
    }
    if (maxWriterConnection != null) {
      return maxWriterConnection;
    } else {
      try {
        maxWriterConnection = connection.getMetaData().getMaxConnections();
        // 0 writer is not really possible
        if (maxWriterConnection == 0) {
          return 1;
        } else {
          return maxWriterConnection;
        }
      } catch (SQLException e) {
        JdbcDataSystemLog.LOGGER_DB_JDBC.severe("Tip: The getMaxConnections is may be not supported on the JDBC driver. Adding it to the extension will resolve this problem.");
        throw new RuntimeException(e);
      }
    }

  }

  public int getDatabaseMajorVersion() {
    try {
      return this.connection.getMetaData().getDatabaseMajorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public int getDatabaseMinorVersion() {
    try {
      return this.connection.getMetaData().getDatabaseMinorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getCurrentSchema() {
    try {

      return this.getCurrentConnection().getSchema();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  public String getCurrentCatalog() {
    try {

      return this.getCurrentConnection().getCatalog();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // A cache object
// integer is data type id
  private Map<Integer, DataType> dataTypeMap = new HashMap<>();

  /**
   * Return a data type by JDBC Type code
   *
   * @param typeCode
   */
  @Override
  public DataType getDataType(Integer typeCode) {

    DataType dataType = dataTypeMap.get(typeCode);

    if (dataType == null) {
      DataTypeDatabase dataTypeDatabase = null;
      JdbcDataStoreExtension jdbcDataStoreExtension = this.getExtension();
      if (jdbcDataStoreExtension != null) {
        dataTypeDatabase = jdbcDataStoreExtension.dataTypeOf(typeCode);
      }

      JdbcDataTypeDriver jdbcDataTypeDriver = this.getDataTypeDriver(typeCode);

      // Get the data type Jdbc
      DataTypeJdbc dataTypeJdbc = DataTypesJdbc.of(typeCode);

      dataType = new DataType.DataTypeBuilder(typeCode)
        .DatabaseDataType(dataTypeDatabase)
        .JdbcDataType(dataTypeJdbc)
        .build();

      dataTypeMap.put(typeCode, dataType);
    }

    return dataType;

  }

  /**
   * Return the data type (info) from the driver
   *
   * @param typeCode the type code
   * @return
   */
  private JdbcDataTypeDriver getDataTypeDriver(Integer typeCode) {

    if (dataTypeInfoMap == null) {
      dataTypeInfoMap = Jdbcs.getDataTypeDriver(getCurrentConnection());
    }
    return dataTypeInfoMap.get(typeCode);

  }

  // The map that contains the data type
  private Map<Integer, JdbcDataTypeDriver> dataTypeDriverMap = new HashMap<>();

  private Collection<JdbcDataTypeDriver> getDataTypeInfos() {
    if (dataTypeDriverMap.size() == 0) {
      // Initialize it by passing a dummy type code - ie 0
      getDataTypeDriver(0);
    }
    return dataTypeDriverMap.values();
  }


  /**
   * Return an object to be set in a prepared statement (for instance)
   * Example: if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as a
   * oracle.sql.BINARY_DOUBLE
   *
   * @param targetColumnType the target column type
   * @param sourceObject     the java object to be loaded
   * @return
   */
  public Object getLoadObject(int targetColumnType, Object sourceObject) {

    Object object = null;
    if (this.getExtension() != null) {
      object = this.getExtension().getLoadObject(targetColumnType, sourceObject);
    }
    if (object == null) {
      return sourceObject;
    } else {
      return object;
    }
  }

  // The map that will contain the driver data type
  private Map<Integer, JdbcDataTypeDriver> dataTypeInfoMap;

  @Override
  public ProcessingEngine getProcessingEngine() {
    if (this.processingEngine == null) {
      this.processingEngine = new JdbcDataProcessingEngine(this);
    }
    return this.processingEngine;
  }

}
