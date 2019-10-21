package net.bytle.db.jdbc;

import net.bytle.db.DbDefaultValue;
import net.bytle.db.connection.URIExtended;
import net.bytle.db.database.*;
import net.bytle.db.jdbc.Hana.SqlDatabaseIHana;
import net.bytle.db.jdbc.Hive.SqlDatabaseIHive;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.jdbc.Oracle.SqlDatabaseIOracle;
import net.bytle.db.jdbc.SqlServer.SqlDatabaseISqlServer;
import net.bytle.db.jdbc.spi.DataTypeDriver;
import net.bytle.db.jdbc.spi.SqlDatabaseI;
import net.bytle.db.jdbc.spi.SqlDatabases;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;
import net.bytle.type.Typess;

import java.io.Closeable;
import java.sql.*;
import java.util.*;

public class JdbcDataSystem extends TableSystem {

    public static final String DB_ORACLE = "Oracle";
    public static final String DB_HANA = "HDB";
    public static final String DB_SQL_SERVER = "Microsoft SQL Server";
    public static final String DB_SQLITE = "SQLite";
    public static final String DB_HIVE = "Apache Hive";

    private SqlDatabaseI sqlDatabaseI;

    // A cache object
    // integer is data type id
    private Map<Integer, DataType> dataTypeMap = new HashMap<>();


    private Connection connection;
    private final Database database;


    public SqlDatabaseI getSqlDatabase() {

        if (sqlDatabaseI == null) {

            String name;
            try {
                name = getCurrentConnection().getMetaData().getDatabaseProductName();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            switch (name) {
                case DB_ORACLE:
                    sqlDatabaseI = new SqlDatabaseIOracle(this);
                    break;
                case DB_HANA:
                    sqlDatabaseI = new SqlDatabaseIHana(this);
                    break;
                case DB_SQL_SERVER:
                    sqlDatabaseI = new SqlDatabaseISqlServer(this);
                    break;
                case DB_SQLITE:
                    sqlDatabaseI = SqlDatabases.getSqlDatabase(database.getUrl());
                    break;
                case DB_HIVE:
                    sqlDatabaseI = new SqlDatabaseIHive(this);
                    break;
            }

        }
        return sqlDatabaseI;
    }

    public JdbcDataSystem(Database database) {
        this.database = database;
        this.connection = null;
    }

    public static JdbcDataSystem of(Database database) {
        return new JdbcDataSystem(database);
    }

    @Override
    public JdbcDataPath getDataPath(DataUri dataUri) {

        return getPrivatelyJdbcPath(dataUri.getPathSegments());

    }

    private JdbcDataPath getPrivatelyJdbcPath(List<String> pathSegments) {

        String currentCatalog;
        try {
            currentCatalog = this.getCurrentConnection().getCatalog();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String currentSchema = this.getCurrentSchema();


        if (pathSegments.size() >= 1) {
            String first = pathSegments.get(0);
            switch (first) {
                case ".":
                    switch (pathSegments.size()) {
                        case 1:
                            return JdbcDataPath.of(this, currentCatalog, currentSchema, null);
                        case 2:
                            return JdbcDataPath.of(this, currentCatalog, currentSchema, pathSegments.get(pathSegments.size() - 1));
                        default:
                            throw new RuntimeException("The working context is the schema and have no children, it's then not possible to have following path (" + String.join("/", pathSegments) + ")");
                    }
                case "..":
                    switch (pathSegments.size()) {
                        case 1:
                            // Catalog
                            return JdbcDataPath.of(this, currentCatalog, null, null);
                        case 2:
                            switch (pathSegments.get(1)) {
                                case "..":
                                    return JdbcDataPath.of(this, null, null, null);
                                case ".":
                                    return JdbcDataPath.of(this, currentCatalog, null, null);
                                default:
                                    return JdbcDataPath.of(this, currentCatalog, pathSegments.get(1), null);
                            }
                        case 3:
                            return JdbcDataPath.of(this, currentCatalog, pathSegments.get(1), pathSegments.get(2));

                        default:
                            throw new RuntimeException("A Jdbc path knows max only three parts (catalog, schema, name). This path is then not possible (" + String.join("/", pathSegments) + ")");
                    }

                default:

                    if (pathSegments.size() > 3) {
                        throw new RuntimeException("This jdbc path (" + String.join("/", pathSegments) + ") is not a valid JDBC uri because it has more than 3 name path but a Jdbc database system supports only maximum three: catalog, schema and name");
                    }

                    String catalog;
                    if (pathSegments.size() > 2) {
                        catalog = pathSegments.get(pathSegments.size() - 3);
                    } else {
                        catalog = currentCatalog;
                    }

                    String schema;
                    if (pathSegments.size() > 1) {
                        schema = pathSegments.get(pathSegments.size() - 2);
                    } else {
                        schema = currentSchema;
                    }

                    return JdbcDataPath.of(this, catalog, schema, pathSegments.get(pathSegments.size() - 1));

            }
        } else {
            throw new RuntimeException("Empty path is not allowed");
        }
    }


    @Override
    public JdbcDataPath getDataPath(String... names) {

        return getPrivatelyJdbcPath(Arrays.asList(names));

    }


    /**
     * @param dataPath
     * @return if the table exist in the underlying database (actually the letter case is important)
     * <p>
     */

    @Override
    public Boolean exists(DataPath dataPath) {

        JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
        boolean tableExist;
        String[] types = {"TABLE"};

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

    @Override
    public SelectStream getSelectStream(DataPath dataPath) {
        JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
        return SqlSelectStream.of(jdbcDataPath);
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
    public void close() throws Exception {


        if (this.connection != null) {
            try {
                this.connection.close();
                JdbcDataSystemLog.LOGGER_DB_JDBC.info("The connection of the database (" + this.database.getDatabaseName() + ") was closed.");
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


        if (this.connection == null) {
            this.connection = getNewConnection(Databases.MODULE_NAME);
        }
        try {
            if (this.connection.isClosed()) {

                // With the database id being the database name, this is not true anymore ?
                // throw new RuntimeException("The connection was closed ! We cannot reopen it otherwise the object id will not be the same anymore");
                JdbcDataSystemLog.LOGGER_DB_JDBC.severe("The database connection was closed ! We reopen it.");
                this.connection = getNewConnection("main");

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
    public synchronized Connection getNewConnection(String appName) {


        URIExtended uriExtended = new URIExtended(this.database.getUrl());
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
        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Trying to connect to the connection (" + database.getUrl() + ")");
        try {

            // connection = DriverManager.getConnection(this.url, this.user, this.password);
            Properties connectionProperties = new Properties();
            // Sql Server
            // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
            //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
            connectionProperties.put("applicationName", DbDefaultValue.LIBRARY_NAME + " " + appName);
            if (database.getUser() != null) {
                connectionProperties.put("user", database.getUser());
                if (database.getPassword() != null) {
                    connectionProperties.put("password", database.getPassword());
                }
            }
            connection = DriverManager.getConnection(database.getUrl(), connectionProperties);

        } catch (SQLException e) {
            String msg = "Unable to connect to the database (" + database.getDatabaseName() + ")with the following URL (" + database.getUrl() + "). Error: " + e.getMessage();
            JdbcDataSystemLog.LOGGER_DB_JDBC.severe(msg);
            throw new RuntimeException(e);
        }


        // Post Connection statement (such as alter session set current_schema)
        if (this.database.getConnectionStatement() != null) {
            try (CallableStatement callableStatement = connection.prepareCall(this.database.getConnectionStatement())) {

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
        if (this.getSqlDatabase() != null) {
            maxWriterConnection = this.getSqlDatabase().getMaxWriterConnection();
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
        return null;
    }

    @Override
    public Integer size(DataPath dataPath) {

        Integer size = 0;
        try (
                SelectStream selectStream = getSelectStream("select count(1) from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath))
        ) {
            Boolean next = selectStream.next();
            if (next) {
                size = selectStream.getInteger(1);
            }
        }
        return size;
    }

    @Override
    public boolean isDataUnit(DataPath dataPath) {
        JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;
        return jdbcDataPath.isDataUnit();
    }

    @Override
    public SelectStream getSelectStream(ColumnDef[] columnDefs) {
        // Only from the same data path test
        assert columnDefs.length >= 1: "The number of columns given must be at minimal one if you want a data stream";
        final DataPath dataPath = columnDefs[0].getRelationDef().getDataPath();
        StringBuilder query = new StringBuilder();
        query.append("select ");
        for (int i = 0; i<columnDefs.length;i++){
            ColumnDef columnDef = columnDefs[i];
            if (!columnDef.getRelationDef().getDataPath().equals(dataPath)){
                throw new RuntimeException("Only a stream of columns of the same data path is for now supported.");
            }
            query.append(JdbcDataSystemSql.getFullyQualifiedSqlName(columnDef));
            if (i<columnDefs.length-1) {
                query.append(", ");
            }
        }
        query.append(" from ");
        query.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
        return getSelectStream(query.toString());
    }


    public Database getDatabase() {
        return database;
    }

    @Override
    public <T> T getMax(ColumnDef<T> columnDef) {

        String columnStatement = columnDef.getColumnName();

        String statementString = "select max(" + columnStatement + ") from " + JdbcDataSystemSql.getFullyQualifiedSqlName(columnDef.getRelationDef().getDataPath());
        try (
                Statement statement = getCurrentConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(statementString);
        ) {
            Object returnValue = null;
            if (resultSet.next()) {
                returnValue = resultSet.getObject(1);
            }
            return (T) returnValue;

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }

    @Override
    public boolean isContainer(DataPath dataPath) {
        return !isDataUnit(dataPath);
    }

    @Override
    public DataPath create(DataPath dataPath) {

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

        return dataPath;


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
            SqlDatabaseI sqlDatabaseI = this.getSqlDatabase();
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
        switch (jdbcDataPath.getType()){
            case JdbcDataPath.TABLE_TYPE:
                dropTableStatement.append("table ");
                break;
            case JdbcDataPath.VIEW_TYPE:
                dropTableStatement.append("view ");
                break;
            default:
                throw new RuntimeException("The drop of the table type ("+jdbcDataPath.getType()+") is not implemented");
        }
        dropTableStatement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
        try (
                Statement statement = getCurrentConnection().createStatement()
        ) {

            statement.execute(dropTableStatement.toString());
            JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table " + dataPath.toString() + " dropped");

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


        StringBuilder truncateTableStatement = new StringBuilder().append("truncate from ");
        truncateTableStatement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));

        try (
                Statement statement = getCurrentConnection().createStatement();
        ) {

            statement.execute(truncateTableStatement.toString());
            JdbcDataSystemLog.LOGGER_DB_JDBC.info("Table (" + dataPath.toString() + ") truncated");

        } catch (SQLException e) {
            System.err.println(truncateTableStatement);
            throw new RuntimeException(e);
        }

    }

    @Override
    public <T> T getMin(ColumnDef<T> columnDef) {

        String columnStatement = columnDef.getColumnName();
        String statementString = "select min(" + columnStatement + ") from " + JdbcDataSystemSql.getFullyQualifiedSqlName(columnDef.getRelationDef().getDataPath());

        try (
                Statement statement = getCurrentConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(statementString);
        ) {
            Object returnValue = null;

            if (resultSet.next()) {
                switch (columnDef.getDataType().getTypeCode()) {
                    case Types.DATE:
                        // In sqllite, getting a date object returns a long
                        returnValue = resultSet.getDate(1);
                        break;
                    default:
                        returnValue = resultSet.getObject(1);
                        break;
                }

            }
            if (returnValue != null) {

                return Typess.safeCast(returnValue, columnDef.getClazz());

            } else {
                return null;
            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }

    @Override
    public void dropForeignKey(ForeignKeyDef foreignKeyDef) {
        /**
         * TODO: move that outside of the core
         * for now a hack
         * because Sqlite does not support alter table drop foreign keys
         */
        if (!this.getProductName().equals(DB_SQLITE)) {
            JdbcDataPath jdbcDataPath = (JdbcDataPath) foreignKeyDef.getTableDef().getDataPath();
            String dropStatement = "alter table " + JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath) + " drop constraint " + foreignKeyDef.getName();
            try {

                Statement statement = getCurrentConnection().createStatement();
                statement.execute(dropStatement);
                statement.close();

                JdbcDataSystemLog.LOGGER_DB_JDBC.info("Foreign Key (" + foreignKeyDef.getName() + ") deleted from the table (" + jdbcDataPath.toString() + ")");

            } catch (SQLException e) {

                System.err.println(dropStatement);
                throw new RuntimeException(e);

            }
        }
    }

    @Override
    public SelectStream getSelectStream(String query) {
        return SqlSelectStream.of(this, query);
    }

    @Override
    public TableSystemProvider getProvider() {
        return null;
    }

    @Override
    public InsertStream getInsertStream(DataPath dataPath) {
        return null;
    }

    @Override
    public List<DataPath> getChildrenDataPath(DataPath dataPath) {


        return DbObjectBuilder.getChildrenDataPath((JdbcDataPath) dataPath);

    }

    @Override
    public DataPath move(DataPath source, DataPath target) {
        throw new RuntimeException("Not yet implemented");
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
        if (this.getSqlDatabase() != null) {
            object = this.getSqlDatabase().getLoadObject(targetColumnType, sourceObject);
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


}
