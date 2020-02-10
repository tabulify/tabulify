package net.bytle.db.jdbc;

import net.bytle.db.Tabular;
import net.bytle.db.database.DataStore;
import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.jdbc.Hana.SqlDatabaseIHana;
import net.bytle.db.jdbc.Hive.SqlDatabaseIHive;
import net.bytle.db.jdbc.SqlServer.SqlDatabaseISqlServer;
import net.bytle.db.jdbc.spi.DataTypeDriver;
import net.bytle.db.jdbc.spi.SqlDatabaseI;
import net.bytle.db.jdbc.spi.SqlDatabases;
import net.bytle.db.model.DataType;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;

import java.io.Closeable;
import java.sql.*;
import java.util.*;

public class JdbcDataSystem extends TableSystem {

  public static final String DB_ORACLE = "Oracle";
  public static final String DB_HANA = "HDB";
  public static final String DB_SQL_SERVER = "Microsoft SQL Server";
  public static final String DB_SQLITE = "SQLite";
  public static final String DB_HIVE = "Apache Hive";

  private SqlDatabaseI sqlDatabase;

  // A cache object
  // integer is data type id
  private Map<Integer, DataType> dataTypeMap = new HashMap<>();


  private Connection connection;
  private final Database database;
  private JdbcDataProcessingEngine processingEngine;


  public SqlDatabaseI getExtension() {

    if (sqlDatabase == null) {

      String name;
      try {
        name = getCurrentConnection().getMetaData().getDatabaseProductName();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      switch (name) {
        case DB_HANA:
          sqlDatabase = new SqlDatabaseIHana(this);
          break;
        case DB_SQL_SERVER:
          sqlDatabase = new SqlDatabaseISqlServer(this);
          break;
        case DB_HIVE:
          sqlDatabase = new SqlDatabaseIHive(this);
          break;
        default:
          sqlDatabase = SqlDatabases.getSqlDatabase(database.getConnectionString());
          break;
      }

    }
    return sqlDatabase;
  }

  public JdbcDataSystem(DataStore datastore) {
    this.database = Database.of(datastore);
    this.connection = null;
  }

  public static JdbcDataSystem of(DataStore database) {
    return new JdbcDataSystem(database);
  }



  @Override
  public JdbcDataPath getDataPath(DataUri dataUri) {
    return JdbcDataPath.of(this,dataUri);
  }


  @Override
  public JdbcDataPath getDataPath(String... names) {

    return getCurrentPath().resolve(names);

  }


  /**
   * @param dataPath
   * @return if the table exist in the underlying database (actually the letter case is important)
   * <p>
   */

  @Override
  public Boolean exists(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;

    switch (jdbcDataPath.getType()) {
      case JdbcDataPath.QUERY_TYPE:
        return true;
      default:
        boolean tableExist;
        String[] types = {"TABLE", "VIEW"};

        final String schemaPattern = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
        try (
          ResultSet tableResultSet = getCurrentConnection()
            .getMetaData()
            .getTables(
              jdbcDataPath.getCatalog(),
              schemaPattern,
              jdbcDataPath.getName(),
              types)
        ) {
          tableExist = tableResultSet.next(); // For TYPE_FORWARD_ONLY
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        return tableExist;
    }


  }

  @Override
  public SelectStream getSelectStream(DataPath dataPath) {
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    return jdbcDataPath.getSelectStream();
  }

  /**
   * Closes this resource, relinquishing any underlying resources.
   * This method is invoked automatically on objects managed by the
   * {@code try}-with-resources statement.
   *
   * <p>While this interface method is declared to throw {@code
   * Exception}, implementers are <em>strongly</em> encouraged to
   * declare concrete implementations of the {@code close} method to
   * throw more specific exceptions, or to throw no exception at all
   * if the close operation cannot fail.
   *
   * <p> Cases where the close operation may fail require careful
   * attention by implementers. It is strongly advised to relinquish
   * the underlying resources and to internally <em>mark</em> the
   * resource as closed, prior to throwing the exception. The {@code
   * close} method is unlikely to be invoked more than once and so
   * this ensures that the resources are released in a timely manner.
   * Furthermore it reduces problems that could arise when the resource
   * wraps, or is wrapped, by another resource.
   *
   * <p><em>Implementers of this interface are also strongly advised
   * to not have the {@code close} method throw {@link
   * InterruptedException}.</em>
   * <p>
   * This exception interacts with a thread's interrupted status,
   * and runtime misbehavior is likely to occur if an {@code
   * InterruptedException} is {@linkplain Throwable#addSuppressed
   * suppressed}.
   * <p>
   * More generally, if it would cause problems for an
   * exception to be suppressed, the {@code AutoCloseable.close}
   * method should not throw it.
   *
   * <p>Note that unlike the {@link Closeable#close close}
   * method of {@link Closeable}, this {@code close} method
   * is <em>not</em> required to be idempotent.  In other words,
   * calling this {@code close} method more than once may have some
   * visible side effect, unlike {@code Closeable.close} which is
   * required to have no effect if called more than once.
   * <p>
   * However, implementers of this interface are strongly encouraged
   * to make their {@code close} methods idempotent.
   *
   * @throws Exception if this resource cannot be closed
   */
  @Override
  public void close()  {


    if (this.connection != null) {
      try {
        this.connection.close();
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("The connection of the database (" + this.database.getName() + ") was closed.");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }


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


    URIExtended uriExtended = new URIExtended(this.database.getConnectionString());
    String driver = uriExtended.getDriver();

    if (driver != null) {
      try {
        Class.forName(driver);
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Driver " + driver + " loaded");
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("The driver Class (" + driver + ") for the database (" + database + ")  could not be loaded. An error occurs: " + e.getMessage() + ". May be that the driver is not on the path ?", e);
      }
    }


    Connection connection;
    JdbcDataSystemLog.LOGGER_DB_JDBC.info("Trying to connect to the connection (" + database.getConnectionString() + ")");
    try {

      // connection = DriverManager.getConnection(this.url, this.user, this.password);
      Properties connectionProperties = new Properties();
      // Sql Server
      // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
      //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
      connectionProperties.put("applicationName", Tabular.APP_NAME + " " + getDataStore().getName()+" "+purpose);
      if (database.getUser() != null) {
        connectionProperties.put("user", database.getUser());
        if (database.getPassword() != null) {
          connectionProperties.put("password", database.getPassword());
        }
      }
      connection = DriverManager.getConnection(database.getConnectionString(), connectionProperties);

    } catch (SQLException e) {
      String msg = "Unable to connect to the database (" + database.getName() + ")with the following URL (" + database.getConnectionString() + "). Error: " + e.getMessage();
      JdbcDataSystemLog.LOGGER_DB_JDBC.severe(msg);
      throw new RuntimeException(e);
    }


    // Post Connection statement (such as alter session set current_schema)
    if (this.database.getPostConnectionStatement() != null) {
      try (CallableStatement callableStatement = connection.prepareCall(this.database.getPostConnectionStatement())) {

        callableStatement.execute();

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

  @Override
  public Boolean isEmpty(DataPath dataPath) {

    throw new UnsupportedOperationException("Not implemented");

  }

  @Override
  public Integer size(DataPath dataPath) {

    Integer size = 0;
    DataPath queryDataPath = dataPath.getDataSystem().getProcessingEngine().getQuery("select count(1) from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    try (
      SelectStream selectStream = getSelectStream(queryDataPath)
    ) {
      Boolean next = selectStream.next();
      if (next) {
        size = selectStream.getInteger(0);
      }
    }
    return size;
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    return jdbcDataPath.isDocument();
  }

  @Override
  public JdbcDataPath getCurrentPath() {
    return JdbcDataPath.of(this,getCurrentCatalog(), getCurrentSchema(),null);
  }


  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    return Jdbcs.getReferencingDataPaths((JdbcDataPath) dataPath);
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    if (this.processingEngine == null) {
      this.processingEngine = new JdbcDataProcessingEngine(this);
    }
    return this.processingEngine;
  }


  @Override
  public Database getDataStore() {
    return database;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return !isDocument(dataPath);
  }

  @Override
  public void create(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;

    // Check that the foreign tables exist
    for (ForeignKeyDef foreignKeyDef : dataPath.getDataDef().getForeignKeys()) {
      DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
      if (!exists(foreignDataPath)) {
        throw new RuntimeException("The foreign table (" + foreignDataPath.toString() + ") does not exist");
      }
    }

    // Standard SQL
    List<String> createTableStatements = DbDdl.getCreateTableStatements(dataPath);
    for (String createTableStatement : createTableStatements) {
      try {

        Statement statement = getCurrentConnection().createStatement();
        statement.execute(createTableStatement);
        statement.close();

      } catch (SQLException e) {
        System.err.println(createTableStatement);
        throw new RuntimeException(e);
      }
    }
    final String name = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : "null";
    JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") created in the schema (" + name + ")");


  }

  @Override
  public String getProductName() {
    try {
      return getCurrentConnection().getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


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
      SqlDatabaseI sqlDatabaseI = this.getExtension();
      if (sqlDatabaseI != null) {
        dataTypeDatabase = sqlDatabaseI.dataTypeOf(typeCode);
      }

      DataTypeDriver dataTypeDriver = this.getDataTypeDriver(typeCode);

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

  @Override
  public void drop(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    StringBuilder dropTableStatement = new StringBuilder();
    dropTableStatement.append("drop ");
    switch (jdbcDataPath.getType()) {
      case JdbcDataPath.TABLE_TYPE:
        dropTableStatement.append("table ");
        break;
      case JdbcDataPath.VIEW_TYPE:
        dropTableStatement.append("view ");
        break;
      default:
        throw new RuntimeException("The drop of the table type (" + jdbcDataPath.getType() + ") is not implemented");
    }
    dropTableStatement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    try (
      Statement statement = getCurrentConnection().createStatement()
    ) {

      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Dropping "+jdbcDataPath.getType()+" " + dataPath.toString());
      statement.execute(dropTableStatement.toString());
      JdbcDataSystemLog.LOGGER_DB_JDBC.info(jdbcDataPath.getType()+" " + dataPath.toString() + " dropped");

    } catch (SQLException e) {
      System.err.println(dropTableStatement);
      throw new RuntimeException(e);
    }

  }

  @Override
  public void delete(DataPath dataPath) {


    String deleteStatement = "delete from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);

    try (
      Statement statement = getCurrentConnection().createStatement();
    ) {
      statement.execute(deleteStatement);
      // Without commit, the database is locked for sqlite (if the connection is no more in autocommit mode)
      getCurrentConnection().commit();
      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table " + dataPath.getDataSystem() + " deleted");
    } catch (SQLException e) {

      throw new RuntimeException(e);
    }

  }

  @Override
  public void truncate(DataPath dataPath) {

    final SqlDatabaseI sqlDatabase = getExtension();
    String truncateStatement;
    if (sqlDatabase != null) {
      truncateStatement = sqlDatabase.getTruncateStatement(dataPath);
    } else {
      StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
      truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
      truncateStatement = truncateStatementBuilder.toString();
    }

    try (
      Statement statement = getCurrentConnection().createStatement();
    ) {

      statement.execute(truncateStatement);
      JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") truncated");

    } catch (SQLException e) {
      System.err.println(truncateStatement);
      throw new RuntimeException(e);
    }

  }


  @Override
  public TableSystemProvider getProvider() {
    return null;
  }

  @Override
  public InsertStream getInsertStream(DataPath dataPath) {
    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
    return SqlInsertStream.of(jdbcDataPath);
  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {


    return Jdbcs.getChildrenDataPath((JdbcDataPath) dataPath);

  }

  /**
   * This function is called by {@link net.bytle.db.spi.Tabulars#move(DataPath, DataPath)}
   * The checks on source and target are already done on the calling function
   *
   * @param source
   * @param target
   * @param transferProperties
   */
  @Override
  public void move(DataPath source, DataPath target, TransferProperties transferProperties) {

    // insert into select statement
    String insertInto = DbDml.getInsertIntoStatement((JdbcDataPath) source, (JdbcDataPath) target);
    try {
      Statement statement = connection.createStatement();
      Boolean resultSetReturned = statement.execute(insertInto);
      if (!resultSetReturned) {
        int updateCount = statement.getUpdateCount();
        JdbcDataSystemLog.LOGGER_DB_JDBC.info(updateCount + " records where moved from (" + source.toString() + ") to (" + target.toString() + ")");
      }
    } catch (SQLException e) {
      final String msg = "Error when executing the insert into statement: " + insertInto;
      JdbcDataSystemLog.LOGGER_DB_JDBC.severe(msg);
      throw new RuntimeException(msg, e);
    }

  }

  // The map that will contain the driver data type
  private Map<Integer, DataTypeDriver> dataTypeInfoMap;

  /**
   * Return the data type (info) from the driver
   *
   * @param typeCode the type code
   * @return
   */
  private DataTypeDriver getDataTypeDriver(Integer typeCode) {

    if (dataTypeInfoMap == null) {
      dataTypeInfoMap = Jdbcs.getDataTypeDriver(getCurrentConnection());
    }
    return dataTypeInfoMap.get(typeCode);

  }

  // The map that contains the data type
  private Map<Integer, DataTypeDriver> dataTypeDriverMap = new HashMap<>();

  private Collection<DataTypeDriver> getDataTypeInfos() {
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

      String schema;
      switch (this.getCurrentConnection().getMetaData().getDatabaseProductName()) {

        case DB_ORACLE:
          // The function getSchema with the Oracle Driver throws an error (abstract ...)
          schema = this.getCurrentConnection().getMetaData().getUserName();
          break;
        default:
          schema = this.getCurrentConnection().getSchema();
          break;
      }

      return schema;

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
}
