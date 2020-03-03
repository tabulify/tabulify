package net.bytle.db.jdbc;

import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An object with all meta information about a JDBC data store
 */
public class SqlDataStore extends DataStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDataStore.class);
  private SqlDataSystem sqlDataSystem;

  @Override
  public SqlDataStore addProperty(String key, String value) {
    super.addProperty(key, value);
    return this;
  }

  @Override
  public SqlDataStore setStrict(boolean strict) {
    super.setStrict(strict);
    return this;
  }

  @Override
  public <T> T getObject(Object object, Class<T> clazz) {
    throw new RuntimeException(Strings.multiline(
      "This function is an extension point if the driver does not support it",
      "It's then data store dependent and should be implemented/overwritten in the data store extension (ie "+this.getProductName()+")"));
  }


  // Current connection
  private Connection connection;

  public static final String DB_HANA = "HDB";
  public static final String DB_SQLITE = "SQLite";
  public static final String DB_HIVE = "Apache Hive";

  // Jdbc
  public static final String DRIVER_PROPERTY_KEY = "driver";
  public static final String POST_STATEMENT_PROPERTY_KEY = "post_statement";
  private JdbcDataStoreExtension sqlDatabase;
  private JdbcDataProcessingEngine processingEngine;


  public SqlDataStore(String name, String url) {
    super(name, url);
  }

  public static SqlDataStore of(String name, String url) {

    return new SqlDataStore(name, url);

  }


  /**
   * This is a JDBC connection parameter
   * It should be threated as {@link #addProperty(String, String)}
   *
   * @param jdbcDriver
   * @return
   */
  public SqlDataStore setDriver(String jdbcDriver) {
    super.addProperty(DRIVER_PROPERTY_KEY, jdbcDriver);
    return this;
  }


  public SqlDataStore setPostConnectionStatement(String connectionScriptValue) {
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
  public SqlDataStore setConnectionString(String connectionString) {

    if (isOpen()) {
      throw new RuntimeException("The connection string cannot be changed while there is a connection. It has already the value (" + this.connectionString + ") and cannot be set to (" + connectionString + ")");
    } else {
      super.setConnectionString(connectionString);
    }
    return this;
  }

  @Override
  public boolean isOpen() {
    return this.connection != null;
  }

  @Override
  public SqlDataStore setUser(String user) {
    super.setUser(user);
    return this;
  }

  @Override
  public SqlDataStore setPassword(String password) {
    super.setPassword(password);
    return this;
  }

  @Override
  public SqlDataSystem getDataSystem() {

    if (sqlDataSystem == null) {
      sqlDataSystem = new SqlDataSystem(this);
    }
    return sqlDataSystem;

  }

  @Override
  public AnsiDataPath getDefaultDataPath(String... names) {
    return this.getCurrentDataPath().resolve(names);
  }

  @Override
  public DataPath getTypedDataPath(String type, String... parts) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public AnsiDataPath getCurrentDataPath() {
    return getSqlDataPath(getCurrentCatalog(), getCurrentSchema(), null);
  }

  public String getProductName() {
    try {
      return getCurrentConnection().getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

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
  public DataPathAbs getQueryDataPath(String query) {
    return new AnsiDataPath(this, query);
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
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Post statement connection executed (" + this.getPostConnectionStatement() + ")");
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
    try {
      Integer maxWriterConnection = connection.getMetaData().getMaxConnections();
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

      String catalog = this.getCurrentConnection().getCatalog();
      if (catalog != null && catalog.equals("")) {
        catalog = null;
      }
      return catalog;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<SqlDataType> getSqlDataTypes() {
    updateSqlDataTypeIfNeeded();
    return super.getSqlDataTypes();
  }

  @Override
  public SqlDataType getSqlDataType(Integer typeCode) {
    updateSqlDataTypeIfNeeded();
    return super.getSqlDataType(typeCode);
  }

  @Override
  public SqlDataType getSqlDataType(Class<?> clazz) {
    updateSqlDataTypeIfNeeded();
    return super.getSqlDataType(clazz);
  }

  @Override
  public SqlDataType getSqlDataType(String typeName) {
    updateSqlDataTypeIfNeeded();
    return super.getSqlDataType(typeName);
  }

  // A breaker to not update the data type each time
  Boolean sqlDataTypeWereUpdated = false;

  private void updateSqlDataTypeIfNeeded() {
    if (!sqlDataTypeWereUpdated) {
      // As soon as we have the connection, we update the sql data type
      // This is because the credential are needed and they are not given at the constructor level
      // because they are not always mandatory
      Map<Integer, SqlDataType> dataTypeInfoMap =
        super.
          getSqlDataTypes()
          .stream()
          .collect(Collectors.toMap(SqlDataType::getTypeCode, dt -> dt));

      ResultSet typeInfoResultSet;
      try {
        typeInfoResultSet = this.getCurrentConnection().getMetaData().getTypeInfo();
        while (typeInfoResultSet.next()) {
          int typeCode = typeInfoResultSet.getInt("DATA_TYPE");
          SqlDataType sqlDataType = dataTypeInfoMap.get(typeCode);
          if (sqlDataType == null) {
            sqlDataType = SqlDataType.of(typeCode);
            this.addSqlDataType(sqlDataType);
          }
          String typeName = typeInfoResultSet.getString("TYPE_NAME");
          sqlDataType.setTypeName(typeName);
          int precision = typeInfoResultSet.getInt("PRECISION");
          sqlDataType.setMaxPrecision(precision);
          String literalPrefix = typeInfoResultSet.getString("LITERAL_PREFIX");
          sqlDataType.setLiteralPrefix(literalPrefix);
          String literalSuffix = typeInfoResultSet.getString("LITERAL_SUFFIX");
          sqlDataType.setLiteralSuffix(literalSuffix);
          String createParams = typeInfoResultSet.getString("CREATE_PARAMS");
          sqlDataType.setCreateParams(createParams);
          Short nullable = typeInfoResultSet.getShort("NULLABLE");
          sqlDataType.setNullable(nullable);
          Boolean caseSensitive = typeInfoResultSet.getBoolean("CASE_SENSITIVE");
          sqlDataType.setCaseSensitive(caseSensitive);
          Short searchable = typeInfoResultSet.getShort("SEARCHABLE");
          sqlDataType.setSearchable(searchable);
          Boolean unsignedAttribute = typeInfoResultSet.getBoolean("UNSIGNED_ATTRIBUTE");
          sqlDataType.setUnsignedAttribute(unsignedAttribute);
          Boolean fixedPrecScale = typeInfoResultSet.getBoolean("FIXED_PREC_SCALE");
          sqlDataType.setFixedPrecScale(fixedPrecScale);
          Boolean autoIncrement = typeInfoResultSet.getBoolean("AUTO_INCREMENT");
          sqlDataType.setAutoIncrement(autoIncrement);
          String localTypeName = typeInfoResultSet.getString("LOCAL_TYPE_NAME");
          sqlDataType.setLocalTypeName(localTypeName);
          Integer minimumScale = Integer.valueOf(typeInfoResultSet.getShort("MINIMUM_SCALE"));
          sqlDataType.setMinimumScale(minimumScale);
          Integer maximumScale = Integer.valueOf(typeInfoResultSet.getShort("MAXIMUM_SCALE"));
          sqlDataType.setMaximumScale(maximumScale);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }

      sqlDataTypeWereUpdated = true;
    }
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

    return sourceObject;

  }


  @Override
  public ProcessingEngine getProcessingEngine() {
    if (this.processingEngine == null) {
      this.processingEngine = new JdbcDataProcessingEngine(this);
    }
    return this.processingEngine;
  }

  /**
   * @return the quote for identifier such as table, column name
   */
  String getIdentifierQuote() {
    String identifierQuoteString = "\"";
    try {
      final Connection currentConnection = this.getCurrentConnection();
      if (currentConnection != null) {
        identifierQuoteString = currentConnection.getMetaData().getIdentifierQuoteString();
      }
    } catch (SQLException e) {
      JdbcDataSystemLog.LOGGER_DB_JDBC.warning("The database (" + this + ") throw an error when retrieving the quoted string identifier." + e.getMessage());
    }
    return identifierQuoteString;
  }

  /**
   * A sql data path has only three parts
   * @param catalog
   * @param schema
   * @param name
   */
  public AnsiDataPath getSqlDataPath(String catalog, String schema, String name) {
    return new AnsiDataPath(this,catalog,schema,name);
  }
}
