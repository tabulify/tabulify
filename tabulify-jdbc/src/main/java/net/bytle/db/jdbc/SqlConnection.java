package net.bytle.db.jdbc;

import net.bytle.db.Tabular;
import net.bytle.db.connection.ConnectionAttValueBooleanDataType;
import net.bytle.db.connection.ConnectionAttValueTimeDataType;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.noop.NoOpConnection;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.exception.*;
import net.bytle.regexp.Glob;
import net.bytle.type.*;
import net.bytle.type.time.Date;
import net.bytle.type.time.Time;
import net.bytle.type.time.Timestamp;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.db.connection.ConnectionAttValueTimeDataType.*;
import static net.bytle.db.jdbc.SqlDataPathType.UNKNOWN;

/**
 * An object with all meta information about a JDBC data store
 */
public class SqlConnection extends NoOpConnection {

  private SqlDataSystem sqlDataSystem;

  /**
   * Prison functionality before a new connection
   * When asking for attributes of a connection
   * Most of them are Driver attributes that needs a connection
   * <p>
   * A list of the connections would then wait a couple of second
   * while trying to make a connection when asking for a list of its attributes
   * To avoid that, we have added a wait time functionality
   * before a reattempt
   */
  private static final long WAIT_TIME_BEFORE_NEXT_CONNECTION_ATTEMPT_SECOND = 60;
  private LocalDateTime failConnectionAttemptAt;


  @Override
  public SqlConnection addVariable(String key, Object value) {

    SqlConnectionAttribute connectionAttribute;
    try {
      connectionAttribute = Casts.cast(key, SqlConnectionAttribute.class);
    } catch (Exception e) {
      super.addVariable(key, value);
      return this;
    }
    if (connectionAttribute.needsConnection()) {
      throw new RuntimeException("The connection attribute (" + connectionAttribute + ") cannot be overwritten as it's a derived attribute");
    }
    Variable variable;
    try {
      variable = getTabular().createVariable(connectionAttribute, value);
    } catch (Exception e) {
      throw new RuntimeException("An error has occurred while creating the connection variable (" + connectionAttribute + ") with the value (" + value + ") for the connection (" + this + "). Error: " + e.getMessage(), e);
    }
    super.addVariable(variable);
    return this;


  }

  @Override
  public SqlConnectionResourcePath createStringPath(String pathOrName, String... names) {


    String path;
    switch (names.length) {
      case 0:
        path = pathOrName;
        break;
      default:
        ArrayList<String> paths = new ArrayList<>();
        paths.add(pathOrName);
        paths.addAll(Arrays.asList(names));
        path = paths.stream()
          .map(s -> this.getDataSystem().createQuotedName(s))
          .collect(Collectors.joining(this.getSeparator()));
        break;
    }
    return SqlConnectionResourcePath.createOfConnectionPath(this, path);

  }

  // Current connection
  private java.sql.Connection connection;

  public static final String DB_HIVE = "Apache Hive";

  // Jdbc
  private SqlDataProcessingEngine processingEngine;

  // Driver
  Map<Integer, SqlDataType> driverDataType = new HashMap<>();


  public SqlConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);

    this.initVariableWhereConnectionIsNotNeeded();

  }

  private void initVariableWhereConnectionIsNotNeeded() {
    initVariable(true);
  }

  private void initVariableWhereConnectionIsNeeded() {
    initVariable(false);
  }

  private void initVariable(boolean needsConnection) {
    for (SqlConnectionAttribute connectionAttribute : SqlConnectionAttribute.values()) {
      if (connectionAttribute.needsConnection() == needsConnection) {
        continue;
      }
      String value;
      switch (connectionAttribute) {
        case DATABASE_PRODUCT_VERSION:
          value = this.getMetadata().getDatabaseProductVersion();
          break;
        case DATABASE_PRODUCT_NAME:
          value = this.getMetadata().getDatabaseProductName();
          break;
        case DATABASE_MAJOR_VERSION:
          value = String.valueOf(this.getMetadata().getDatabaseMajorVersion());
          break;
        case DATABASE_MINOR_VERSION:
          value = String.valueOf(this.getMetadata().getDatabaseMinorVersion());
          break;
        case JDBC_MINOR_VERSION:
          value = String.valueOf(this.getMetadata().getJdbcMinorVersion());
          break;
        case JDBC_MAJOR_VERSION:
          value = String.valueOf(this.getMetadata().getJdbcMajorVersion());
          break;
        case DRIVER_VERSION:
          value = this.getMetadata().getDriverVersion();
          break;
        case DRIVER_NAME:
          value = this.getMetadata().getDriverName();
          break;
        case SUPPORT_BATCH_UPDATES:
          value = String.valueOf(this.getMetadata().getSupportBatchUpdates());
          break;
        case SUPPORT_NAMED_PARAMETERS:
          value = String.valueOf(this.getMetadata().getSupportNamedParameters());
          break;
        default:
          value = null;
          break;
      }
      try {
        Variable variable = this.getTabular().createVariable(connectionAttribute, value);
        this.addVariable(variable);
      } catch (Exception e) {
        // should not happen as there is no vault encryption
        throw new RuntimeException(e);
      }
    }
  }


  /**
   * This is a JDBC connection parameter
   * It should be threated as {@link #addVariable(String, Object)}
   *
   * @param jdbcDriver - the driver
   * @return the connection for chaining
   */
  public SqlConnection setDriver(String jdbcDriver) {
    super.addVariable(SqlConnectionAttribute.DRIVER, jdbcDriver);
    return this;
  }


  public SqlConnection setPostConnectionStatement(String connectionScriptValue) {
    super.addVariable(SqlConnectionAttribute.CONNECTION_INIT_SCRIPT, connectionScriptValue);
    return this;
  }


  public String getDriver() {

    try {
      return super.getVariable(SqlConnectionAttribute.DRIVER).getValueOrDefault().toString();
    } catch (NoVariableException | NoValueException e) {
      return "";
    }

  }

  public String getPostConnectionStatement() throws NotFoundException {

    try {
      return (String) super.getVariable(SqlConnectionAttribute.CONNECTION_INIT_SCRIPT).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new NotFoundException();
    }

  }


  @Override
  public boolean isOpen() {
    return this.connection != null;
  }

  @Override
  public SqlConnection setUser(String user) {
    super.setUser(user);
    return this;
  }

  @Override
  public SqlConnection setPassword(String password) {
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
  public SqlDataPath getDataPath(String pathOrName) {
    return getDataPath(pathOrName, UNKNOWN);
  }

  /**
   * @param pathOrName the path or a name
   * @param mediaType  - the media type is not really needed in Sql
   * @return the path
   */
  @Override
  public SqlDataPath getDataPath(String pathOrName, MediaType mediaType) {


    return new SqlDataPath(this, pathOrName, mediaType);


  }


  @Override
  public String getCurrentPathCharacters() {

    throw new InternalException("Tabli does not support relative SQL path, there is no current characters");
    // it could be `/`

  }

  @Override
  public String getParentPathCharacters() {

    throw new InternalException("Tabli does not support relative SQL path");
    // it could be `//`

  }

  @Override
  public String getSeparator() {
    return "."; //  You can not find it in the driver metadata, this is a sql standard;
  }


  /**
   * The current data path in a sql database is the schema
   * (in a file system, it's the current working directory)
   *
   * @return the current sql path
   */
  @Override
  public SqlDataPath getCurrentDataPath() {
    String currentCatalog;
    try {
      currentCatalog = getCurrentCatalog();
    } catch (NoCatalogException e) {
      currentCatalog = null;
    }
    return getSqlSchemaDataPath(currentCatalog, getCurrentSchema());
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
    try {
      if (this.connection != null) {

        this.connection.close();
        SqlLog.LOGGER_DB_JDBC.info("The connection of the database (" + this.getName() + ") was closed.");

      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public SqlDataPath createScriptDataPath(DataPath dataPath) {
    return new SqlDataPath(this, dataPath);
  }


  /**
   * Return the current Connection
   *
   * @return the current connection or null (if no URL)
   * <p>
   * The current connection is the first connection created
   */
  public java.sql.Connection getCurrentConnection() {


    try {

      if (this.connection != null) {
        if (!this.connection.isClosed()) {
          return this.connection;
        }
        // With the database id being the database name, this is not true anymore ?
        // throw new RuntimeException("The connection was closed ! We cannot reopen it otherwise the object id will not be the same anymore");
        SqlLog.LOGGER_DB_JDBC.severe("The database connection was closed ! We recreate it.");
      }

      this.connection = getNewConnection();
      initVariableWhereConnectionIsNeeded();

      return this.connection;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * synchronized because it's used within thread
   *
   * @return return a new connection object
   */
  public synchronized java.sql.Connection getNewConnection() {


    SqlUri sqlUri = new SqlUri(this.getUriAsString());
    String driver = sqlUri.getDriver();

    if (driver != null) {
      try {
        Class.forName(driver);
        SqlLog.LOGGER_DB_JDBC.info("Driver " + driver + " loaded");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("The driver Class (" + driver + ") for the database (" + this.getName() + ")  could not be loaded. An error occurs: " + e.getMessage() + ". May be that the driver is not on the path ?", e);
      }
    }


    java.sql.Connection connection;
    Properties connectionProperties = Maps.toProperties(this.getVariablesAsKeyValueMap());
    SqlLog.LOGGER_DB_JDBC.info("Trying to connect to the connection (" + this.getUriAsVariable() + ")");
    try {

      boolean connectionTryInPrison = false;
      long durationInPrison = 0;
      if (this.failConnectionAttemptAt != null) {
        durationInPrison = ChronoUnit.SECONDS.between(this.failConnectionAttemptAt, LocalDateTime.now());
        if (durationInPrison > WAIT_TIME_BEFORE_NEXT_CONNECTION_ATTEMPT_SECOND) {
          this.failConnectionAttemptAt = null;
        } else {
          connectionTryInPrison = true;
        }
      }
      if (!connectionTryInPrison) {

        // Timeout
        // DriverManager.setLoginTimeout(1);
        connection = DriverManager.getConnection(this.getUriAsString(), connectionProperties);

        SqlLog.LOGGER_DB_JDBC.info("Connected !");

        // Post Connection statement (such as alter session set current_schema)
        try {
          String postConnectionStatement = this.getPostConnectionStatement();
          try (CallableStatement callableStatement = connection.prepareCall(postConnectionStatement)) {
            callableStatement.execute();
            SqlLog.LOGGER_DB_JDBC.info("Post statement connection executed (" + postConnectionStatement + ")");
          } catch (SQLException e) {
            throw new RuntimeException("Post Connection error occurs: " + e.getMessage(), e);
          }
        } catch (NotFoundException e) {
          // ok
        }


        return connection;

      } else {
        long waitTime = WAIT_TIME_BEFORE_NEXT_CONNECTION_ATTEMPT_SECOND - durationInPrison;
        throw new SQLException("The connection last attempt was " + durationInPrison + " seconds ago, waiting " + waitTime + " second before a new attempt.");
      }

    } catch (SQLException e) {
      String msg = "Unable to connect to the database (" + this.getName() + ") with the following URL (" + this.getUriAsVariable() + "). Error: " + e.getMessage();
      SqlLog.LOGGER_DB_JDBC.severe(msg);
      if (this.failConnectionAttemptAt == null) {
        this.failConnectionAttemptAt = LocalDateTime.now();
      }
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

      if (this.getMetadata().isSchemaSeenAsCatalog()) {
        return this.getCurrentConnection().getCatalog();
      } else {
        return this.getCurrentConnection().getSchema();
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  public String getCurrentCatalog() throws NoCatalogException {
    try {

      if (this.getMetadata().isSchemaSeenAsCatalog()) {
        throw new NoCatalogException("Catalog is not supported");
      }

      String catalog = this.getCurrentConnection().getCatalog();
      if (catalog == null || (catalog.isEmpty()
      )) {
        throw new NoCatalogException("Catalog is not supported");
      }
      return catalog;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<SqlDataType> getSqlDataTypes() {
    updateSqlDataTypeIfNeeded();
    return new HashSet<>(driverDataType.values());
  }

  @Override
  public SqlDataType getSqlDataType(Integer typeCode) {
    updateSqlDataTypeIfNeeded();
    SqlDataType sqlDataType = driverDataType.get(typeCode);
    if (sqlDataType != null) {

      return sqlDataType;
    } else {
      return super.getSqlDataType(typeCode);
    }
  }

  @Override
  public SqlDataType getSqlDataType(Class<?> clazz) {
    return super.getSqlDataType(clazz);
  }

  @Override
  public SqlDataType getSqlDataType(String typeName) {
    updateSqlDataTypeIfNeeded();
    SqlDataType sqlDataType = driverDataType.values()
      .stream()
      .filter(dt -> dt.getSqlName().equalsIgnoreCase(typeName))
      .findFirst()
      .orElse(null);
    if (sqlDataType != null) {
      /**
       * Even if we could return it back directly
       * we don't because, there is
       * some processing that occurs such as the class
       * in the function {@link #getSqlDataType(Integer)}
       */
      return getSqlDataType(sqlDataType.getTypeCode());
    } else {
      return super.getSqlDataType(typeName);
    }
  }

  // A breaker to not update the data type each time
  Boolean sqlDataTypeWereUpdated = false;

  private void updateSqlDataTypeIfNeeded() {
    if (!sqlDataTypeWereUpdated) {
      // As soon as we have the connection, we update the sql data type
      // This is because the credential are needed and they are not given at the constructor level
      // because they are not always mandatory

      /**
       * Because of alias (ie NUMERIC=REAL for instance), we are looping
       * on the type code in order to create the data type and this alias
       * as two different {@link SqlDataType}
       */
      Map<Integer, SqlMetaDataType> map = this.getDataSystem().getMetaDataTypes();
      for (Integer typeCode : map.keySet()) {
        SqlMetaDataType sqlMetaDataType = map.get(typeCode);
        SqlDataType sqlDataType = driverDataType.computeIfAbsent(typeCode, type -> SqlDataType.creationOf(this, type));
        // Clazz mapping cannot be null
        if (sqlMetaDataType.getSqlClass() != null) {
          sqlDataType.setSqlJavaClazz(sqlMetaDataType.getSqlClass());
        } else {
          SqlDataType superSqlDataType = super.getSqlDataType(sqlMetaDataType.getTypeCode());
          if (superSqlDataType != null && superSqlDataType.getSqlClass() != null) {
            sqlDataType.setSqlJavaClazz(superSqlDataType.getSqlClass());
          }
        }
        Boolean mandatoryPrecision = sqlMetaDataType.getMandatoryPrecision();
        if (mandatoryPrecision != null) {
          sqlDataType.setMandatoryPrecision(mandatoryPrecision);
        }
        Integer maxPrecision = sqlMetaDataType.getMaxPrecision();
        if (maxPrecision != null) {
          sqlDataType.setMaxPrecision(maxPrecision);
        }
        sqlDataType
          .setDriverTypeCode(sqlMetaDataType.getDriverTypeCode())
          .setSqlName(sqlMetaDataType.getTypeName())
          .setMaximumScale(sqlMetaDataType.getMaximumScale())
          .setAutoIncrement(sqlMetaDataType.getAutoIncrement())
          .setMinimumScale(sqlMetaDataType.getMinimumScale())
          .setCaseSensitive(sqlMetaDataType.getCaseSensitive())
          .setCreateParams(sqlMetaDataType.getCreateParams())
          .setFixedPrecisionScale(sqlMetaDataType.getFixedPrecisionScale())
          .setLiteralPrefix(sqlMetaDataType.getLiteralPrefix())
          .setLiteralSuffix(sqlMetaDataType.getLiteralSuffix())
          .setNullable(sqlMetaDataType.getNullable())
          .setLocalTypeName(sqlMetaDataType.getLocalTypeName())
          .setSearchable(sqlMetaDataType.getSearchable())
          .setUnsignedAttribute(sqlMetaDataType.getUnsignedAttribute())
          .setDefaultPrecision(sqlMetaDataType.getDefaultPrecision());

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
   * @return the object to load into the target database
   */
  public Object getLoadObject(int targetColumnType, Object sourceObject) {

    return sourceObject;

  }


  @Override
  public ProcessingEngine getProcessingEngine() {
    if (this.processingEngine == null) {
      this.processingEngine = new SqlDataProcessingEngine(this);
    }
    return this.processingEngine;
  }


  /**
   * To get a data path when the different name are known
   * This is the entry point that should be overwritten by the
   * datastore extension.
   *
   * @param catalog - may be null - may be null if not supported
   * @param schema  - may be null - may be null if not supported
   * @param name    - may be null if schema or cat
   * @return the sql path
   */
  public SqlDataPath createSqlDataPath(String catalog, String schema, String name) {
    return createSqlDataPath(catalog, schema, name, null);
  }

  /**
   * A sql schema data path has only two parts
   * A shortcut function that create the Sql data path and set its type at once
   *
   * @param catalog the catalog
   * @param schema  the schema
   */
  public SqlDataPath getSqlSchemaDataPath(String catalog, String schema) {
    return createSqlDataPath(catalog, schema, null, SqlDataPathType.SCHEMA);
  }


  /**
   * Return a java sql object to be set in a prepared statement (for instance)
   * before insertion
   * <p>
   * Example:
   * * if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as a oracle.sql.BINARY_DOUBLE
   * * if you want to load a string into a bigint, you need to transform it
   *
   * @param targetColumnType the target column type
   * @return an Java SQL object to be loaded in a prepared statement
   * @throws CastException - We throw because we don't have any context
   * @see #toSqlString(Object, SqlDataType) the string representation
   */
  public Object toSqlObject(Object sourceObject, SqlDataType targetColumnType) throws CastException {

    Class<?> sqlClass = targetColumnType.getSqlClass();
    if (sqlClass.equals(java.sql.SQLXML.class)) {
      try {
        SQLXML xmlVal = this.connection.createSQLXML();
        xmlVal.setString(sourceObject.toString());
        return xmlVal;
      } catch (SQLException e) {
        throw new RuntimeException("We cannot create a SQLXML for the datastore (" + this + ")", e);
      }
    }

    /**
     *
     * Date/timestamp are saved as:
     *   * sql text
     *   * or Epoch Ms
     * <p>
     * even if the documentation states https://sqlite.org/datatype3.html
     * that the Unix Time is the number of **seconds** since 1970-01-01 00:00:00 UTC.
     * because the driver save them at the ms level
     * when passing a java.sql.Date or java.sql.Timestamp
     *
     */

    if (targetColumnType.getTypeCode() == Types.DATE) {
      ConnectionAttValueTimeDataType dateDataType = this.getMetadata().getDateDataTypeOrDefault();
      switch (dateDataType) {
        case NATIVE:
          return Date.createFromObject(sourceObject).toSqlDate();
        case SQL_LITERAL:
          return Date.createFromObject(sourceObject).toSqlDate().toString();
        case EPOCH_MS:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Date.createFromObject(sourceObject).toEpochMillis();
          }
        case EPOCH_SEC:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Date.createFromObject(sourceObject).toEpochSec();
          }
        case EPOCH_DAY:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Date.createFromObject(sourceObject).toEpochDay();
          }
      }
    } else if (targetColumnType.getTypeCode() == Types.TIMESTAMP) {
      ConnectionAttValueTimeDataType timestampDataType = this.getMetadata().getTimestampDataType();
      switch (timestampDataType) {
        case NATIVE:
          return Timestamp.createFromObject(sourceObject).toSqlTimestamp();
        case SQL_LITERAL:
          return Timestamp.createFromObject(sourceObject).toSqlTimestamp().toString();
        case EPOCH_MS:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Timestamp.createFromObject(sourceObject).toEpochMilli();
          }
        case EPOCH_SEC:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Timestamp.createFromObject(sourceObject).toEpochSec();
          }
      }
    } else if (targetColumnType.getTypeCode() == Types.TIME) {
      ConnectionAttValueTimeDataType timeDataType = this.getMetadata().getTimeDataType();
      switch (timeDataType) {
        case NATIVE:
          return Time.createFromObject(sourceObject).toSqlTime();
        case SQL_LITERAL:
          return Time.createFromObject(sourceObject).toSqlTime().toString();
        case EPOCH_MS:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Time.createFromObject(sourceObject).toEpochMilli();
          }
        case EPOCH_SEC:
          if (sourceObject instanceof Integer || sourceObject instanceof Long) {
            return sourceObject;
          } else {
            return Time.createFromObject(sourceObject).toEpochSec();
          }
      }
    } else if (targetColumnType.getTypeCode() == Types.BOOLEAN) {

      ConnectionAttValueBooleanDataType dateDataType = this.getMetadata().getBooleanDataType();
      switch (dateDataType) {
        case Native:
          return Booleans.createFromObject(sourceObject).toBoolean();
        case Binary:
          try {
            return Booleans.createFromObject(sourceObject).toInteger();
          } catch (NullValueException e) {
            return null;
          }
      }


    }

    return Casts.cast(sourceObject, sqlClass);


  }

  @SuppressWarnings("unchecked")
  @Override
  public List<SqlDataPath> select(String globPathOrName, MediaType mediaType) {

    return this.getDataSystem().select(getCurrentDataPath(), globPathOrName, mediaType);
  }

  /**
   * This function will return only tables (no schema, no catalog)
   * <p>
   * Only metadata entries matching the search pattern are returned.
   * <p>
   * If a search pattern argument is set to null, that argument's criterion will be dropped from the search.
   *
   * @param schemaPattern    - a {@link Glob#toSqlPattern(String)} Sql Pattern}
   * @param catalogPattern   - a {@link Glob#toSqlPattern(String)} Sql Pattern}
   * @param tableNamePattern - a {@link Glob#toSqlPattern(String)} Sql Pattern}
   * @return This is a wrapper around the function {@link DatabaseMetaData#getTables(String, String, String, String[])}
   * @see <a href="https://github.com/dbeaver/dbeaver/issues/5618">also Issue 5618</a>
   */
  public List<SqlDataPath> getTables(String catalogPattern, String schemaPattern, String tableNamePattern) {


    /**
     * Check if there is any weird implementation
     * such as MySql where the schema is in the catalog value
     */
    boolean schemaSeenAsCatalog = this.getMetadata().isSchemaSeenAsCatalog();
    if (!schemaSeenAsCatalog) {
      /**
       * Default driver Jdbc call
       */
      return this.getTablesJdbc(catalogPattern, schemaPattern, tableNamePattern);
    }

    /**
     * Schema as catalog
     * The tables function {@link DatabaseMetaData#getTables(String, String, String, String[])}
     * does not allow filtering on catalog pattern
     * we implement it below
     */
    List<SqlDataPath> jdbcDataPaths = new ArrayList<>();
    try {
      ResultSet catalogsResultSet = this.getCurrentConnection().getMetaData().getCatalogs();
      /**
       * The column name may be uppercase or lowercase
       * We catch that by asking the column name
       */
      String catalogColumnName = catalogsResultSet.getMetaData().getColumnName(1);
      Glob schemaGlob = Glob.createOfSqlPattern(schemaPattern);
      while (catalogsResultSet.next()) {
        String catName = catalogsResultSet.getString(catalogColumnName);
        if (schemaGlob.matches(catName)) {
          jdbcDataPaths.addAll(this.getTablesJdbc(catName, null, tableNamePattern));
        }
      }
    } catch (SQLException throwable) {
      throw new RuntimeException(throwable);
    }
    return jdbcDataPaths;

  }

  /**
   * The JDBC driver getTables where the catalog is not a patter
   *
   * @param catalogSearchName the catalog (not a patter)
   * @param schemaPattern     the schema pattern to search
   * @param tableNamePattern  the table pattern to search
   * @return a list of sql path
   */
  private List<SqlDataPath> getTablesJdbc(String catalogSearchName, String schemaPattern, String tableNamePattern) {

    List<SqlDataPath> jdbcDataPaths = new ArrayList<>();
    try {

      ResultSet tableResultSet = this.getCurrentConnection().getMetaData().getTables(
        catalogSearchName,
        schemaPattern,
        tableNamePattern,
        null);
      /**
       * The column name may be uppercase or lowercase
       * We catch that by asking the column name
       */
      String tableCatalogColumnName = tableResultSet.getMetaData().getColumnName(1);
      String tableSchemaColumnName = tableResultSet.getMetaData().getColumnName(2);
      String tableNameColumnName = tableResultSet.getMetaData().getColumnName(3);
      String tableTypeColumnName = tableResultSet.getMetaData().getColumnName(4);
      String tableRemarksColumnName = tableResultSet.getMetaData().getColumnName(5);

      /**
       * Loop
       */
      while (tableResultSet.next()) {
        String schema_name;
        String cat_name;
        schema_name = tableResultSet.getString(tableSchemaColumnName);
        cat_name = tableResultSet.getString(tableCatalogColumnName);

        final String table_name = tableResultSet.getString(tableNameColumnName);
        final String type_name = tableResultSet.getString(tableTypeColumnName);
        final String remarks = tableResultSet.getString(tableRemarksColumnName);

        SqlDataPathType objectType;
        try {
          objectType = SqlDataPathType.getSqlType(type_name);
        } catch (NotSupportedException e) {
          // the table type is not supported by tabli, we don't add it
          // index for instance
          continue;
        }

        /**
         * Driver may return a null catalog value
         */
        if (cat_name == null) {
          if (catalogSearchName != null) {
            if (!Glob.createOf(catalogSearchName).containsSqlMatchers()) {
              // This is not a sql pattern, it was just used to select the schemas
              cat_name = catalogSearchName;
            } else {
              throw new IllegalStateException("During the select of tables, the connection  (" + this + ") returns a catalog with a null value value with the SQL Pattern (" + catalogSearchName + ")");
            }
          } else {
            try {
              cat_name = this.getCurrentCatalog();
            } catch (NoCatalogException e) {
              // null then, not supported
            }
          }
        }


        SqlDataPath childDataPath = (SqlDataPath) this
          .createSqlDataPath(cat_name, schema_name, table_name, objectType)
          .setDescription(remarks);
        jdbcDataPaths.add(childDataPath);


      }
      return jdbcDataPaths;

    } catch (
      SQLException e) {
      throw new RuntimeException(e);
    }

  }

  public SqlDataPath createSqlDataPath(String catalog, String schema, String objectName, SqlDataPathType mediaType) {

    SqlConnectionResourcePath resourcePath = SqlConnectionResourcePath
      .createOfCatalogSchemaAndObjectName(this, catalog, schema, objectName)
      .toRelative();
    return getDataPath(resourcePath.toString(), mediaType);

  }


  public List<SqlDataPath> getSchemas(String catalogSqlPattern, String schemaSqlPattern) {

    List<SqlDataPath> jdbcDataPaths = new ArrayList<>();
    try {

      /**
       * Normal behavior
       * Schema is not seen as a catalog
       */
      if (!this.getMetadata().isSchemaSeenAsCatalog()) {
        ResultSet tableResultSet = this.getCurrentConnection().getMetaData().getSchemas(
          catalogSqlPattern,
          schemaSqlPattern);
        /**
         * The column name may be uppercase or lowercase
         * We catch that by asking the column name
         */
        String schemaNameColumnName = tableResultSet.getMetaData().getColumnName(1);
        String schemaCatalogColumnName = tableResultSet.getMetaData().getColumnName(2);
        /**
         * Loop
         */
        while (tableResultSet.next()) {
          String schema_name = tableResultSet.getString(schemaNameColumnName);
          String cat_name = tableResultSet.getString(schemaCatalogColumnName);

          /**
           * Driver may return null
           */
          if (cat_name == null && catalogSqlPattern != null) {
            if (!Glob.createOf(catalogSqlPattern).containsSqlMatchers()) {
              // This is not a sql pattern, it was just used to select the schemas
              cat_name = catalogSqlPattern;
            } else {
              throw new IllegalStateException("During the select of schemas, the datastore (" + this + ") returns a catalog with a null value value with the SQL Pattern (" + catalogSqlPattern + ")");
            }
          }
          SqlDataPath childDataPath = this
            .getSqlSchemaDataPath(cat_name, schema_name);

          jdbcDataPaths.add(childDataPath);
        }
        return jdbcDataPaths;
      }

      /**
       * Schema is null for the driver
       * Catalog is seen as schema
       */
      ResultSet catalogsResultSet = this.getCurrentConnection().getMetaData().getCatalogs();
      /**
       * The column name may be uppercase or lowercase
       * We catch that by asking the column name
       */
      String catalogColumnName = catalogsResultSet.getMetaData().getColumnName(1);
      Glob schemaGlob = Glob.createOfSqlPattern(schemaSqlPattern);
      while (catalogsResultSet.next()) {
        String cat_name = catalogsResultSet.getString(catalogColumnName);
        if (schemaGlob.matches(cat_name)) {
          SqlDataPath childDataPath = this
            .getSqlSchemaDataPath(null, cat_name);
          jdbcDataPaths.add(childDataPath);
        }
      }
      return jdbcDataPaths;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * @return the configuration object
   */
  @Override
  public SqlConnectionMetadata getMetadata() {
    return new SqlConnectionMetadata(this);
  }


  /**
   * @param catalog the catalog name
   * @return a catalog object path
   */
  public SqlDataPath getSqlCatalogDataPath(String catalog) {
    return createSqlDataPath(catalog, null, null, SqlDataPathType.CATALOG);
  }


  /**
   * @param objectInserted the object to insert
   * @param targetDataType the target data type
   * @return a sql string representation of the object that will be used in an insert statement
   * @see #toSqlObject(Object, SqlDataType) - the sql object representation
   */
  public String toSqlString(Object objectInserted, SqlDataType targetDataType) {

    if (objectInserted == null) {
      return null;
    }
    switch (targetDataType.getTypeCode()) {
      case Types.DATE:

        ConnectionAttValueTimeDataType dateDataType = this.getMetadata().getDateDataTypeOrDefault();
        switch (dateDataType) {
          case SQL_LITERAL:
          case NATIVE:
            /**
             * We return the SQL Date string
             * Postgres would prefer {@link Timestamp#toIsoString()} but on a date level, they are just the same
             */
            return Date.createFromObject(objectInserted).toSqlDate().toString();
          case EPOCH_MS:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Date.createFromObject(objectInserted).toEpochMillis().toString();
            }
          case EPOCH_SEC:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Date.createFromObject(objectInserted).toEpochSec().toString();
            }
          case EPOCH_DAY:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Date.createFromObject(objectInserted).toEpochDay().toString();
            }
          default:
            throw new IllegalStateException("The date data type storage (" + dateDataType + ") has no processing. A developer should add one.");
        }


      case Types.TIMESTAMP:
        ConnectionAttValueTimeDataType timestampDataType = this.getMetadata().getTimestampDataType();
        switch (timestampDataType) {
          case SQL_LITERAL:
          case NATIVE:
            /**
             * We return the SQL Timestamp string
             * ie 2020-11-17 00:00:00.0
             * <p>
             * Postgres would prefer {@link Timestamp#toIsoString()} ie 2020-11-17T00:00
             */
              try {
                  return Timestamp.createFromObject(objectInserted).toSqlTimestamp().toString();
              } catch (CastException e) {
                  throw new RuntimeException(e);
              }
            case EPOCH_MS:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
                try {
                    return Timestamp.createFromObject(objectInserted).toEpochMilli().toString();
                } catch (CastException e) {
                    throw new RuntimeException(e);
                }
            }
          case EPOCH_SEC:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
                try {
                    return Timestamp.createFromObject(objectInserted).toEpochSec().toString();
                } catch (CastException e) {
                    throw new RuntimeException(e);
                }
            }
          case EPOCH_DAY:
            throw new IllegalStateException("The timestamp data type storage (" + timestampDataType + ") is not valid for a timestamp. You can choose (" + SQL_LITERAL + "," + NATIVE + "," + EPOCH_MS + "," + EPOCH_SEC + ")");
          default:
            throw new IllegalStateException("The date data type storage (" + timestampDataType + ") has no processing. A developer should add one.");
        }

      case Types.TIME:
        ConnectionAttValueTimeDataType timeDataType = this.getMetadata().getTimeDataType();
        switch (timeDataType) {
          case SQL_LITERAL:
          case NATIVE:
            return Time.createFromObject(objectInserted).toSqlTime().toString();
          case EPOCH_MS:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Time.createFromObject(objectInserted).toEpochMilli().toString();
            }
          case EPOCH_SEC:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Time.createFromObject(objectInserted).toEpochSec().toString();
            }
          case EPOCH_DAY:
            throw new IllegalStateException("The time data type storage (" + timeDataType + ") is not valid for a timestamp. You can choose (" + SQL_LITERAL + "," + NATIVE + "," + EPOCH_MS + "," + EPOCH_SEC + ")");
          default:
            throw new IllegalStateException("The time data type storage (" + timeDataType + ") has no processing. A developer should add one.");
        }
      case Types.BOOLEAN:
        ConnectionAttValueBooleanDataType booleanDataType = this.getMetadata().getBooleanDataType();
        switch (booleanDataType) {
          case Native:
            return Booleans.createFromObject(objectInserted).toSqlString();
          case Binary:
            try {
              return Booleans.createFromObject(objectInserted).toInteger().toString();
            } catch (NullValueException e) {
              return null;
            }
          default:
            throw new IllegalStateException("The boolean data type storage (" + booleanDataType + ") has no processing. A developer should add one.");
        }
    }
    return objectInserted.toString();
  }

  @Override
  public SqlDataPath getAndCreateRandomDataPath() {
    return (SqlDataPath) super.getAndCreateRandomDataPath();
  }


  public Clob createClob() {
    try {
      return this.connection.createClob();
    } catch (SQLException e) {
      return SqlClob.create();
    }
  }


  /**
   * A utility function to create a query data path(select)
   * Was originally created for test purpose
   *
   * @param sourceDataPath - the table or view
   * @param columnNames    - the column to add in the select
   * @return the sql path
   */
  public SqlDataPath createSelectDataPath(SqlDataPath sourceDataPath, String[] columnNames, String whereClause, String
    orderBy) {

    DataPath scriptDataPath = this.getTabular().getAndCreateRandomMemoryDataPath()
      .setContent("select " + Arrays.stream(columnNames)
        .map(sourceDataPath.getConnection().getDataSystem()::createQuotedName)
        .collect(Collectors.joining(", "))
        + " from " + sourceDataPath.toSqlStringPath()
        + (whereClause != null ? " where " + whereClause : "")
        + (orderBy != null ? " order by " + orderBy : "")
        + ";");
    return createScriptDataPath(scriptDataPath);

  }


  protected String getURL() {
    try {
      return this.getCurrentConnection().getMetaData().getURL();
    } catch (SQLException throwable) {
      throw new RuntimeException(throwable);
    }
  }
}
