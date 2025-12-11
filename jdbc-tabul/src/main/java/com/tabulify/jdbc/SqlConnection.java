package com.tabulify.jdbc;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.ConnectionAttValueBooleanDataType;
import com.tabulify.connection.ConnectionAttValueTimeDataType;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.glob.Glob;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.noop.NoOpConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.ProcessingEngine;
import com.tabulify.exception.*;
import com.tabulify.type.*;
import com.tabulify.type.time.Date;
import com.tabulify.type.time.Time;
import com.tabulify.type.time.Timestamp;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tabulify.connection.ConnectionAttValueTimeDataType.*;

/**
 * An object with all meta information about a JDBC data store
 */
public class SqlConnection extends NoOpConnection {


  private SqlCache sqlCache;

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
  public SqlConnection addAttribute(KeyNormalizer name, Object value, Origin origin) {

    SqlConnectionAttributeEnum connectionAttribute;
    try {
      connectionAttribute = Casts.cast(name, SqlConnectionAttributeEnum.class);
    } catch (Exception e) {
      super.addAttribute(name, value, origin);
      return this;
    }
    if (connectionAttribute.needsConnection()) {
      throw new RuntimeException("The connection attribute (" + connectionAttribute + ") cannot be overwritten as it's a derived attribute");
    }
    Attribute attribute;
    try {
      attribute = getTabular().getVault().createAttribute(connectionAttribute, value, origin);
    } catch (Exception e) {
      throw new RuntimeException("An error has occurred while creating the connection variable (" + connectionAttribute + ") with the value (" + value + ") for the connection (" + this + "). Error: " + e.getMessage(), e);
    }
    super.addAttribute(attribute);
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
    return SqlConnectionResourcePath.createOfConnectionPath(this, path, null);

  }

  // Current connection
  private java.sql.Connection driverConnection;

  public static final String DB_HIVE = "Apache Hive";

  // Jdbc
  private SqlDataProcessingEngine processingEngine;


  public SqlConnection(Tabular tabular, com.tabulify.conf.Attribute name, com.tabulify.conf.Attribute url) {
    super(tabular, name, url);
    this.initVariableWhereConnectionIsNotNeeded();
    this.addAttributesFromEnumAttributeClass(SqlConnectionAttributeEnum.class);

    // Should be after attribute initialization
    Boolean builderCacheEnabled = (Boolean) this.getAttribute(SqlConnectionAttributeEnum.BUILDER_CACHE_ENABLED).getValueOrDefault();
    this.sqlCache = new SqlCache(builderCacheEnabled);

  }

  @Override
  public List<Class<? extends ConnectionAttributeEnum>> getAttributeEnums() {
    ArrayList<Class<? extends ConnectionAttributeEnum>> enums = new ArrayList<>(super.getAttributeEnums());
    enums.add(SqlConnectionAttributeEnum.class);
    return enums;
  }


  private void initVariableWhereConnectionIsNotNeeded() {
    initDerivedVariable(true);
  }

  private void initVariableWhereConnectionIsNeeded() {
    initDerivedVariable(false);
  }

  private void initDerivedVariable(boolean needsConnection) {
    for (SqlConnectionAttributeEnum connectionAttribute : SqlConnectionAttributeEnum.values()) {
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
        Attribute attribute = this.getTabular().getVault().createAttribute(connectionAttribute, value, Origin.DEFAULT);
        this.addAttribute(attribute);
      } catch (Exception e) {
        // should not happen as there is no vault encryption
        throw new RuntimeException(e);
      }
    }
  }


  /**
   * @param jdbcDriver - the driver
   * @return the connection for chaining
   */
  public SqlConnection setDriver(String jdbcDriver) {
    super.addAttribute(SqlConnectionAttributeEnum.DRIVER, jdbcDriver, Origin.DEFAULT);
    return this;
  }


  public String getDriver() {

    return super.getAttribute(SqlConnectionAttributeEnum.DRIVER).getValueOrDefault().toString();

  }

  public String getLoginStatements() {

    return (String) super.getAttribute(SqlConnectionAttributeEnum.LOGIN_STATEMENTS).getValueOrDefault();


  }

  public SqlConnection setLoginStatements(String postConnectionStatement) {

    super.getAttribute(SqlConnectionAttributeEnum.LOGIN_STATEMENTS).setPlainValue(postConnectionStatement);
    return this;


  }


  @Override
  public boolean isOpen() {
    return this.driverConnection != null;
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
    return getDataPath(pathOrName, (MediaType) null);
  }

  /**
   * A data path supplier sot that we can implement the {@link SqlCache}
   */
  protected Supplier<SqlDataPath> getDataPathSupplier(String pathOrName, SqlMediaType mediaType) {
    return () -> new SqlDataPath(this, pathOrName, null, mediaType);
  }

  /**
   * The main entry point to create a data path,
   * Due to the {@link SqlCache}, Connection should not overwrite this function but {@link #getDataPathSupplier(String, SqlMediaType)}
   *
   * @param pathOrName the path or a name
   * @param mediaType  - the media type is not really needed in Sql
   * @return the path
   */
  @Override
  public SqlDataPath getDataPath(String pathOrName, MediaType mediaType) {

    pathOrName = this.getDataSystem().createNormalizedName(pathOrName);
    SqlMediaType sqlMediaType;
    if (mediaType == null) {
      sqlMediaType = SqlMediaType.OBJECT;
    } else {
      sqlMediaType = SqlMediaType.castsToSqlType(mediaType);
    }
    return this.sqlCache.createDataPath(pathOrName, sqlMediaType, getDataPathSupplier(pathOrName, sqlMediaType));

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
   * Don't override it, override instead {@link #getCurrentSchema()} and {@link #getCurrentCatalog()}
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

  public String getDatabaseName() {
    try {
      return getCurrentJdbcConnection().getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }


  @Override
  public void close() {
    try {
      if (this.driverConnection != null) {

        this.driverConnection.close();
        this.driverConnection = null;
        SqlLog.LOGGER_DB_JDBC.info("The connection of the database (" + this.getName() + ") was closed.");

      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public SqlRequest getRuntimeDataPath(DataPath dataPath, MediaType mediaType) {

    return SqlRequest.builder()
      .setExecutableDataPath(this, dataPath)
      .build();

  }

  @Override
  public SqlRequest getRuntimeDataPath(String code) {
    return SqlRequest
      .builder()
      .setSql(this, code)
      .build();
  }

  public java.sql.Connection getCurrentJdbcConnection() {
    return getCurrentJdbcConnection(WAIT_TIME_BEFORE_NEXT_CONNECTION_ATTEMPT_SECOND);
  }

  /**
   * Return the current Connection
   *
   * @param prisonDurationInSec - the time in second that a new attempt should not be executed after a try
   * @return the current connection or null (if no URL)
   * <p>
   * The current connection is the first connection created
   */
  public java.sql.Connection getCurrentJdbcConnection(long prisonDurationInSec) {

    try {

      if (this.driverConnection != null) {
        if (!this.driverConnection.isClosed()) {
          return this.driverConnection;
        }
        // With the database id being the database name, this is not true anymore ?
        // throw new RuntimeException("The connection was closed ! We cannot reopen it otherwise the object id will not be the same anymore");
        SqlLog.LOGGER_DB_JDBC.warning("The database connection was closed ! We recreate it.");
      }

      this.driverConnection = getNewJdbcConnection(prisonDurationInSec);
      initVariableWhereConnectionIsNeeded();

      return this.driverConnection;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  public synchronized java.sql.Connection getNewJdbcConnection() {
    return getNewJdbcConnection(WAIT_TIME_BEFORE_NEXT_CONNECTION_ATTEMPT_SECOND);
  }

  /**
   * synchronized because it's used within thread
   *
   * @param maxPrisonDurationInSec - the amount in sec before trying to connect a new time. If set to zero, disable any error message if the connection is unsuccessful
   * @return return a new connection object
   */
  public synchronized java.sql.Connection getNewJdbcConnection(long maxPrisonDurationInSec) {

    String driver = getAttribute(SqlConnectionAttributeEnum.DRIVER).getValueOrDefaultAsStringNotNull();
    if (driver.isEmpty()) {
      JdbcUri jdbcUri = new JdbcUri(this.getUri().toUri());
      driver = jdbcUri.getDriver();
    }

    if (driver != null) {
      try {
        Class.forName(driver);
        SqlLog.LOGGER_DB_JDBC.info("Driver " + driver + " loaded");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("The driver Class (" + driver + ") for the database (" + this.getName() + ")  could not be loaded. An error occurs: " + e.getMessage() + ". May be that the driver is not on the path ?", e);
      }
    }


    java.sql.Connection connection;


    SqlLog.LOGGER_DB_JDBC.info("Trying to connect to the connection (" + this.getUriAsVariable() + ")");
    try {

      boolean connectionTryInPrison = false;
      long actualDurationInPrison = 0;
      if (maxPrisonDurationInSec > 0 && this.failConnectionAttemptAt != null) {
        actualDurationInPrison = ChronoUnit.SECONDS.between(this.failConnectionAttemptAt, LocalDateTime.now());
        if (actualDurationInPrison > maxPrisonDurationInSec) {
          this.failConnectionAttemptAt = null;
        } else {
          connectionTryInPrison = true;
        }
      }
      if (!connectionTryInPrison) {

        // Timeout
        // DriverManager.setLoginTimeout(1);
        Properties connectionProperties = Maps.toProperties(this.getConnectionProperties());
        connection = DriverManager.getConnection(this.getAttribute(ConnectionAttributeEnumBase.URI).getValueOrDefaultAsStringNotNull(), connectionProperties);

        SqlLog.LOGGER_DB_JDBC.info("Connected !");

        // Post Connection statement (such as alter session set current_schema)
        String postConnectionStatement = this.getLoginStatements();
        if (postConnectionStatement != null && !postConnectionStatement.isEmpty()) {
          try (CallableStatement callableStatement = connection.prepareCall(postConnectionStatement)) {
            callableStatement.execute();
            SqlLog.LOGGER_DB_JDBC.info("Post statement connection executed (" + postConnectionStatement + ")");
          } catch (SQLException e) {
            throw new IllegalArgumentException("Post Connection Login Statements error occurs: " + e.getMessage(), e);
          }
        }

        return connection;

      } else {
        long waitTime = maxPrisonDurationInSec - actualDurationInPrison;
        throw new SQLException("The connection last attempt was " + actualDurationInPrison + " seconds ago, waiting " + waitTime + " second before a new attempt.");
      }

    } catch (SQLException e) {
      if (maxPrisonDurationInSec != 0) {
        String msg = "Unable to connect to the database (" + this.getName() + ") with the following URL (" + this.getUriAsVariable() + "). Error: " + e.getMessage();
        SqlLog.LOGGER_DB_JDBC.severe(msg);
      }
      if (this.failConnectionAttemptAt == null) {
        this.failConnectionAttemptAt = LocalDateTime.now();
      }
      throw new RuntimeException(e);
    }


  }


  public int getDatabaseMajorVersion() {
    try {
      return this.driverConnection.getMetaData().getDatabaseMajorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public int getDatabaseMinorVersion() {
    try {
      return this.driverConnection.getMetaData().getDatabaseMinorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getCurrentSchema() {
    try {

      if (this.getMetadata().isSchemaSeenAsCatalog()) {
        return this.getCurrentJdbcConnection().getCatalog();
      } else {
        return this.getCurrentJdbcConnection().getSchema();
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

      /**
       * Postgres supports a fictive catalog called `postgres`
       * We don't use it as it has no added value
       */
      if (!this.getMetadata().supportsCatalogsInSqlStatementPath()) {
        throw new NoCatalogException("Catalog is not supported");
      }

      String catalog = this.getCurrentJdbcConnection().getCatalog();
      if (catalog == null || (catalog.isEmpty())) {
        throw new NoCatalogException("Catalog is not supported");
      }
      return catalog;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
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
    return createSqlDataPath(catalog, schema, null, SqlMediaType.SCHEMA);
  }

  /**
   * Return a java sql object to be set in a prepared statement (for instance)
   * before insertion
   * <p>
   * Example:
   * * if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as an oracle.sql.BINARY_DOUBLE
   * * if you want to load a string into a bigint, you need to transform it
   *
   * @param targetColumnType the target column type
   * @return an Java SQL object to be loaded in a prepared statement
   * @throws CastException - We throw because we don't have any context
   * @see #toSqlString(Object, ColumnDef) the string representation
   */
  public Object toSqlObject(Object sourceObject, SqlDataType<?> targetColumnType) throws CastException {

    if (sourceObject == null) {
      return null;
    }

    Class<?> sqlClass = targetColumnType.getValueClass();
    if (sqlClass.equals(java.sql.SQLXML.class)) {
      try {
        SQLXML xmlVal = this.driverConnection.createSQLXML();
        String xmlStringValue;
        if (sourceObject instanceof SQLXML) {
          /**
           * Case of our own SqlXmlFromString in common type
           * When the data is XML from file, we wrap it into it
           */
          xmlStringValue = ((SQLXML) sourceObject).getString();
        } else {
          xmlStringValue = sourceObject.toString();
        }
        xmlVal.setString(xmlStringValue);
        return xmlVal;
      } catch (SQLException e) {
        throw new RuntimeException("We cannot create a SQLXML for the connection (" + this + ")", e);
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
    switch (targetColumnType.getVendorTypeNumber()) {
      case Types.DATE:
        ConnectionAttValueTimeDataType dateDataType = this.getMetadata().getDateDataTypeOrDefault();
        switch (dateDataType) {
          case NATIVE:
            return Date.createFromObject(sourceObject).toSqlDate();
          case SQL_LITERAL:
            return Date.createFromObject(sourceObject).toSqlDate().toString();
          case EPOCH_MS:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Date.createFromObject(sourceObject).toEpochMillis();
          case EPOCH_SEC:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Date.createFromObject(sourceObject).toEpochSec();
          case EPOCH_DAY:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Date.createFromObject(sourceObject).toEpochDay();
          default:
            throw new MissingSwitchBranch("dateDataType", dateDataType);
        }
      case Types.TIMESTAMP:
        ConnectionAttValueTimeDataType timestampDataType = this.getMetadata().getTimestampDataType();
        switch (timestampDataType) {
          case NATIVE:
            return Timestamp.createFromObject(sourceObject).toSqlTimestamp();
          case SQL_LITERAL:
            return Timestamp.createFromObject(sourceObject).toSqlTimestamp().toString();
          case EPOCH_MS:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Timestamp.createFromObject(sourceObject).toEpochMilli();
          case EPOCH_SEC:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Timestamp.createFromObject(sourceObject).toEpochSec();
          case EPOCH_DAY:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Date.createFromObject(sourceObject).toEpochDay();
          default:
            throw new MissingSwitchBranch("timestampDataType", timestampDataType);
        }
      case Types.TIME:
        ConnectionAttValueTimeDataType timeDataType = this.getMetadata().getTimeDataType();
        switch (timeDataType) {
          case NATIVE:
            return Time.createFromObject(sourceObject).toSqlTime();
          case SQL_LITERAL:
            return Time.createFromObject(sourceObject).toSqlTime().toString();
          case EPOCH_MS:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Time.createFromObject(sourceObject).toEpochMilli();
          case EPOCH_SEC:
            if (sourceObject instanceof Integer || sourceObject instanceof Long) {
              return sourceObject;
            }
            return Time.createFromObject(sourceObject).toEpochSec();
          case EPOCH_DAY:
            throw new IllegalArgumentException("You can't choose " + EPOCH_DAY + " time storage for the time type.");
          default:
            throw new MissingSwitchBranch("timeDataType", timeDataType);
        }
      case Types.BOOLEAN:
        ConnectionAttValueBooleanDataType boolDataType = this.getMetadata().getBooleanDataType();
        switch (boolDataType) {
          case Native:
            return Booleans.createFromObject(sourceObject).toBoolean();
          case Binary:
            try {
              return Booleans.createFromObject(sourceObject).toInteger();
            } catch (NullValueException e) {
              return null;
            }
          default:
            throw new MissingSwitchBranch("boolDataType", boolDataType);
        }
      case Types.BIT:
        // boolean (We don't support bit array)
        return Booleans.createFromObject(sourceObject).toBoolean();
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
      return this.getSqlTableFromJdbc(catalogPattern, schemaPattern, tableNamePattern);
    }

    /**
     * Schema as catalog
     * The tables function {@link DatabaseMetaData#getTables(String, String, String, String[])}
     * does not allow filtering on catalog pattern
     * we implement it below
     */
    List<SqlDataPath> sqlDataPathsSelected = new ArrayList<>();
    try {
      ResultSet catalogsResultSet = this.getCurrentJdbcConnection().getMetaData().getCatalogs();
      /**
       * The column name may be uppercase or lowercase
       * We catch that by asking the column name
       */
      String catalogColumnName = catalogsResultSet.getMetaData().getColumnName(1);
      Glob schemaGlob = Glob.createOfSqlPattern(schemaPattern);
      while (catalogsResultSet.next()) {
        String catName = catalogsResultSet.getString(catalogColumnName);
        if (schemaGlob.matches(catName)) {
          List<SqlDataPath> sqlDataPath = this.getSqlTableFromJdbc(catName, null, tableNamePattern);
          sqlDataPathsSelected.addAll(sqlDataPath);
        }
      }
    } catch (SQLException throwable) {
      throw new RuntimeException(throwable);
    }
    return sqlDataPathsSelected;

  }

  /**
   * The JDBC driver getTables where the catalog is not a patter
   *
   * @param catalogSearchName the catalog (not a pattern)
   * @param schemaPattern     the schema pattern to search
   * @param tableNamePattern  the table pattern to search
   * @return a list of sql path
   */
  private List<SqlDataPath> getSqlTableFromJdbc(String catalogSearchName, String schemaPattern, String tableNamePattern) {

    List<SqlDataPath> jdbcDataPaths = new ArrayList<>();
    try {

      ResultSet tableResultSet = this.getCurrentJdbcConnection().getMetaData().getTables(
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

        SqlMediaType objectType;
        try {
          objectType = SqlMediaType.castsToSqlType(type_name);
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

        /**
         * Create the data path
         */
        if (this.getMetadata().isSchemaSeenAsCatalog()) {
          if (schema_name != null) {
            throw new InternalException("Catalog is seen as schema but the schema name is not null");
          }
          schema_name = cat_name;
          cat_name = null;
        }
        SqlDataPath childDataPath = (SqlDataPath) this
          .createSqlDataPath(cat_name, schema_name, table_name, objectType)
          .setComment(remarks);

        /**
         * Add
         */
        jdbcDataPaths.add(childDataPath);


      }
      return jdbcDataPaths;

    } catch (
      SQLException e) {
      throw new RuntimeException(e);
    }

  }

  public SqlDataPath createSqlDataPath(String catalog, String schema, String objectName, SqlMediaType mediaType) {

    String compactPath = SqlConnectionResourcePath
      .createOfCatalogSchemaAndObjectName(this, catalog, schema, objectName)
      .toRelative()
      .toString();
    return getDataPath(compactPath, mediaType);

  }


  public List<SqlDataPath> getSchemas(String catalogSqlPattern, String schemaSqlPattern) {

    List<SqlDataPath> jdbcDataPaths = new ArrayList<>();
    try {

      /**
       * Normal behavior
       * Schema is not seen as a catalog
       */
      if (!this.getMetadata().isSchemaSeenAsCatalog()) {
        ResultSet tableResultSet = this.getCurrentJdbcConnection().getMetaData().getSchemas(
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
      ResultSet catalogsResultSet = this.getCurrentJdbcConnection().getMetaData().getCatalogs();
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
  @SuppressWarnings("unused")
  public SqlDataPath getSqlCatalogDataPath(String catalog) {
    return createSqlDataPath(catalog, null, null, SqlMediaType.CATALOG);
  }


  /**
   * @param objectInserted the object to insert
   * @param columnDef      the target column
   * @return a sql string representation of the object that will be used in an insert statement
   * @see #toSqlObject(Object, SqlDataType) - the sql object representation
   */
  public String toSqlString(Object objectInserted, ColumnDef<?> columnDef) {

    if (objectInserted == null) {
      return null;
    }

    SqlDataTypeAnsi ansiType = columnDef.getAnsiType();
    switch (ansiType) {
      case DATE:

        ConnectionAttValueTimeDataType dateDataType = this.getMetadata().getDateDataTypeOrDefault();
        switch (dateDataType) {
          case SQL_LITERAL:
          case NATIVE:
            /**
             * We return the SQL Date string
             * Postgres would prefer {@link Timestamp#toIsoString()} but on a date level, they are just the same
             */
            return Date.createFromObjectSafeCast(objectInserted).toSqlDate().toString();
          case EPOCH_MS:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Date.createFromObjectSafeCast(objectInserted).toEpochMillis().toString();
            }
          case EPOCH_SEC:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Date.createFromObjectSafeCast(objectInserted).toEpochSec().toString();
            }
          case EPOCH_DAY:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            } else {
              return Date.createFromObjectSafeCast(objectInserted).toEpochDay().toString();
            }
          default:
            throw new IllegalStateException("The date data type storage (" + dateDataType + ") has no processing. A developer should add one.");
        }


      case TIMESTAMP:
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
            return Timestamp.createFromObjectSafeCast(objectInserted).toSqlTimestamp().toString();

          case EPOCH_MS:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            }
            return Timestamp.createFromObjectSafeCast(objectInserted).toEpochMilli().toString();
          case EPOCH_SEC:
            if (objectInserted instanceof Long || objectInserted instanceof Integer) {
              return objectInserted.toString();
            }
            return Timestamp.createFromObjectSafeCast(objectInserted).toEpochSec().toString();

          case EPOCH_DAY:
            throw new IllegalStateException("The timestamp data type storage (" + timestampDataType + ") is not valid for a timestamp. You can choose (" + SQL_LITERAL + "," + NATIVE + "," + EPOCH_MS + "," + EPOCH_SEC + ")");
          default:
            throw new IllegalStateException("The date data type storage (" + timestampDataType + ") has no processing. A developer should add one.");
        }

      case TIME:
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
      case BOOLEAN:
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


  /**
   * A utility function to create a query data path(select)
   * Was originally created for test purpose
   *
   * @param sourceDataPath - the table or view
   * @param columnNames    - the column to add in the select
   * @return the sql path
   */
  public SqlDataPath createSelectDataPath(SqlDataPath sourceDataPath,
                                          String[] columnNames, String whereClause, String
                                            orderBy) {

    String query = "select " + Arrays.stream(columnNames)
      .map(sourceDataPath.getConnection().getDataSystem()::createQuotedName)
      .collect(Collectors.joining(", "))
      + " from " + sourceDataPath.toSqlStringPath()
      + (whereClause != null ? " where " + whereClause : "")
      + (orderBy != null ? " order by " + orderBy : "")
      + ";";
    DataPath scriptDataPath = this.getTabular().getAndCreateRandomMemoryDataPath()
      .setContent(query);
    return this.getRuntimeDataPath(scriptDataPath, null);

  }


  protected String getURL() {
    try {
      return this.getCurrentJdbcConnection().getMetaData().getURL();
    } catch (SQLException throwable) {
      throw new RuntimeException(throwable);
    }
  }

  @Override
  public Map<String, Object> getConnectionProperties() {
    Map<String, Object> connectionProperties = super.getConnectionProperties();
    Object user = this.getUser().getValueOrDefault();
    if (user != null) {
      connectionProperties.put("user", user.toString());
    }
    Object password = this.getPassword();
    if (password != null) {
      connectionProperties.put("password", password.toString());
    }
    return connectionProperties;
  }

  @Override
  public Boolean ping() {
    try {
      return this.getCurrentJdbcConnection(0).isValid(0);
    } catch (Exception e) {
      return false;
    }
  }

  public SqlCache getCache() {
    return this.sqlCache;
  }

  public SqlConnection setEnabledCache(boolean bool) {
    Attribute attribute = this.getAttribute(SqlConnectionAttributeEnum.BUILDER_CACHE_ENABLED);
    Boolean cached = (Boolean) attribute.getValueOrDefault();
    if (bool == cached) {
      return this;
    }
    attribute.setPlainValue(bool);
    sqlCache = new SqlCache(bool);
    return this;
  }

  /**
   * Hook to overwrite the driver on resultSet retrieval
   * Why
   * * The resultSet and the clazz? SQL Server even with an XML data type default to returning a LONGVARCHAR if we don't give the clazz in the {@link ResultSet#getObject(int)}, so we need to pass the clazz
   * * Correcting value: Sqlite driver returns false for empty string
   */
  public <T> T getObjectFromResulSet(ResultSet resultSet, ColumnDef<?> columnDef, Class<T> clazz) {
    try {
      return resultSet.getObject(columnDef.getColumnPosition(), clazz);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }


}
