package net.bytle.db.jdbc;

import net.bytle.db.DbDefaultValue;
import net.bytle.db.connection.URIExtended;
import net.bytle.db.database.*;
import net.bytle.db.database.Hana.SqlDatabaseIHana;
import net.bytle.db.database.Hive.SqlDatabaseIHive;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.database.Oracle.SqlDatabaseIOracle;
import net.bytle.db.database.SqlServer.SqlDatabaseISqlServer;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JdbcDataSystem extends TableSystem {

    public static final String DB_ORACLE = "Oracle";
    public static final String DB_HANA = "HDB";
    public static final String DB_SQL_SERVER = "Microsoft SQL Server";
    public static final String DB_SQLITE = "SQLite";
    public static final String DB_HIVE = "Apache Hive";
    public static final String DB_ANSI = "Ansi";

    private SqlDatabaseI sqlDatabaseI;

    // A cache object
    // integer is data type id
    private Map<Integer, DataType> dataTypeMap = new HashMap<>();


    private Connection connection;
    private final Database database;



    protected SqlDatabaseI getSqlDatabase() {

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

    public static TableSystem of(Database database) {
        return new JdbcDataSystem(database);
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {

        if (dataUri.getPathSegments().size() > 3) {
            throw new RuntimeException("This URI ("+dataUri+") is not a valid JDBC uri because it has more than 3 name path but a Jdbc database system supports only maximum three: catalog, schema and name");
        }

        String catalog;
        if (dataUri.getPathSegments().size() > 2) {
            catalog = dataUri.getPathSegment(dataUri.getPathSegments().size() - 3);
        } else {
            try {
                catalog = this.getCurrentConnection().getCatalog();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        String schema;
        if (dataUri.getPathSegments().size() > 1) {
            schema = dataUri.getPathSegment(dataUri.getPathSegments().size() - 2);
        } else {
            schema = this.getCurrentSchema();
        }

        return JdbcDataPath.of(this, catalog,schema,dataUri.getDataName());
    }

    @Override
    public Boolean exists(DataPath dataPath) {
        return null;
    }

    @Override
    public SelectStream getSelectStream(DataPath dataPath) {
        return null;
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

        try {
            Class.forName(driver);
            JdbcDataSystemLog.LOGGER_DB_JDBC.info("Driver " + driver + " loaded");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The driver Class (" + driver + ") for the database (" + database + ")  could not be loaded. An error occurs: " + e.getMessage() + ". May be that the driver is not on the path ?", e);
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


    public Database getDatabase() {
        return database;
    }

    /**
     * Return a data type by JDBC Type code
     *
     * @param typeCode
     */
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
            dataTypeInfoMap = new HashMap<>();
            ResultSet typeInfoResultSet;
            try {
                typeInfoResultSet = connection.getMetaData().getTypeInfo();
                while (typeInfoResultSet.next()) {
                    DataTypeDriver.DataTypeInfoBuilder typeInfoBuilder = new DataTypeDriver.DataTypeInfoBuilder(typeInfoResultSet.getInt("DATA_TYPE"));
                    String typeName = typeInfoResultSet.getString("TYPE_NAME");
                    typeInfoBuilder.typeName(typeName);
                    int precision = typeInfoResultSet.getInt("PRECISION");
                    typeInfoBuilder.maxPrecision(precision);
                    String literalPrefix = typeInfoResultSet.getString("LITERAL_PREFIX");
                    typeInfoBuilder.literalPrefix(literalPrefix);
                    String literalSuffix = typeInfoResultSet.getString("LITERAL_SUFFIX");
                    typeInfoBuilder.literalSuffix(literalSuffix);
                    String createParams = typeInfoResultSet.getString("CREATE_PARAMS");
                    typeInfoBuilder.createParams(createParams);
                    Short nullable = typeInfoResultSet.getShort("NULLABLE");
                    typeInfoBuilder.nullable(nullable);
                    Boolean caseSensitive = typeInfoResultSet.getBoolean("CASE_SENSITIVE");
                    typeInfoBuilder.caseSensitive(caseSensitive);
                    Short searchable = typeInfoResultSet.getShort("SEARCHABLE");
                    typeInfoBuilder.searchable(searchable);
                    Boolean unsignedAttribute = typeInfoResultSet.getBoolean("UNSIGNED_ATTRIBUTE");
                    typeInfoBuilder.unsignedAttribute(unsignedAttribute);
                    Boolean fixedPrecScale = typeInfoResultSet.getBoolean("FIXED_PREC_SCALE");
                    typeInfoBuilder.fixedPrecScale(fixedPrecScale);
                    Boolean autoIncrement = typeInfoResultSet.getBoolean("AUTO_INCREMENT");
                    typeInfoBuilder.autoIncrement(autoIncrement);
                    String localTypeName = typeInfoResultSet.getString("LOCAL_TYPE_NAME");
                    typeInfoBuilder.localTypeName(localTypeName);
                    Integer minimumScale = Integer.valueOf(typeInfoResultSet.getShort("MINIMUM_SCALE"));
                    typeInfoBuilder.minimumScale(minimumScale);
                    Integer maximumScale = Integer.valueOf(typeInfoResultSet.getShort("MAXIMUM_SCALE"));
                    typeInfoBuilder.maximumScale(maximumScale);
                    DataTypeDriver dataTypeDriver = typeInfoBuilder.build();
                    dataTypeInfoMap.put(dataTypeDriver.getTypeCode(), dataTypeDriver);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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
     * Todo: Add {@link DatabaseMetaData#getClientInfoProperties()}
     */
    public void printDatabaseInformation() {

        System.out.println("Information about the database (" + database.getDatabaseName() + "):");

        System.out.println();
        System.out.println("Driver Information:");
        DatabaseMetaData databaseMetadata = null;
        try {
            databaseMetadata = connection.getMetaData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            System.out.println("getDatabaseProductVersion: " + databaseMetadata.getDatabaseProductVersion());

            System.out.println("getDatabaseProductName: " + databaseMetadata.getDatabaseProductName());
            System.out.println("getDatabaseMajorVersion: " + databaseMetadata.getDatabaseMajorVersion());
            System.out.println("getDatabaseMinorVersion: " + databaseMetadata.getDatabaseMinorVersion());
            System.out.println("getMaxConnections: " + databaseMetadata.getMaxConnections());
            System.out.println("getJDBCMajorVersion: " + databaseMetadata.getJDBCMajorVersion());
            System.out.println("getJDBCMinorVersion: " + databaseMetadata.getJDBCMinorVersion());
            System.out.println("getURL: " + databaseMetadata.getURL());
            System.out.println("Driver Version: " + databaseMetadata.getDriverVersion());
            System.out.println("Driver Name: " + databaseMetadata.getDriverName());
            System.out.println("getUserName: " + databaseMetadata.getUserName());
            System.out.println("supportsNamedParameters: " + databaseMetadata.supportsNamedParameters());
            System.out.println("supportsBatchUpdates: " + databaseMetadata.supportsBatchUpdates());
            System.out.println();
            System.out.println("Connection");
            System.out.println("Catalog: " + this.connection.getCatalog());
            String schema;
            if (databaseMetadata.getJDBCMajorVersion() >= 7) {
                schema = this.connection.getSchema();
            } else {
                schema = "The JDBC Driver doesn't have this information.";
            }
            System.out.println("Schema: " + schema);
            System.out.println("Schema Current Connection: " + this.connection.getSchema());
            System.out.println("Client Info");
            Properties clientInfos = this.connection.getClientInfo();
            if (clientInfos != null && clientInfos.size() != 0) {
                for (String key : clientInfos.stringPropertyNames()) {
                    System.out.println("  * (" + key + ") = (" + clientInfos.getProperty(key) + ")");
                }
            } else {
                System.out.println("   * No client infos");
            }

            System.out.println();
            URI url;
            try {
                url = new URI(database.getUrl());
                URIExtended uriExtended = new URIExtended(url);
                System.out.println("URL (" + url + ")");
                System.out.println("Authority: " + url.getAuthority());
                System.out.println("Scheme: " + url.getScheme());
                System.out.println("Scheme Specific Part: " + url.getSchemeSpecificPart());
                System.out.println("Fragment: " + url.getFragment());
                System.out.println("Host: " + url.getHost());
                System.out.println("Path: " + url.getPath());
                System.out.println("Query: " + url.getQuery());
                System.out.println("Raw Query: " + url.getRawQuery());
                System.out.println("Raw Authority: " + url.getRawAuthority());
                System.out.println("Raw Fragment: " + url.getRawFragment());
                System.out.println("Raw Path: " + url.getRawPath());
                System.out.println("Raw Schema Specific Part: " + url.getRawSchemeSpecificPart());
                System.out.println("Driver: " + uriExtended.getDriver());
                System.out.println("Server: " + uriExtended.getServer());
            } catch (URISyntaxException e) {
                System.out.println("Error while reading the URI information. Message:" + e.getMessage());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Print data type given by the driver
     */
    public void printDataTypeInformation() {


        // Headers
        System.out.println("Data Type\t" +
                "Type Name\t" +
                "Precision\t" +
                "literalPrefix\t" +
                "literalSuffix\t" +
                "createParams\t" +
                "nullable\t" +
                "caseSensitive\t" +
                "searchable\t" +
                "unsignedAttribute\t" +
                "fixedPrecScale\t" +
                "localTypeName\t" +
                "minimumScale\t" +
                "maximumScale"
        );

        // Data Type Info
        Collection<DataTypeDriver> dataTypeDrivers = this.getDataTypeInfos();

        for (DataTypeDriver typeInfo : dataTypeDrivers) {
            System.out.println(
                    typeInfo.getTypeCode() + "\t" +
                            typeInfo.getTypeName() + "\t" +
                            typeInfo.getMaxPrecision() + "\t" +
                            typeInfo.getLiteralPrefix() + "\t" +
                            typeInfo.getLiteralSuffix() + "\t" +
                            typeInfo.getCreateParams() + "\t" +
                            typeInfo.getNullable() + "+\t" +
                            typeInfo.getCaseSensitive() + "\t" +
                            typeInfo.getSearchable() + "\t" +
                            typeInfo.getUnsignedAttribute() + "\t" +
                            typeInfo.getFixedPrecScale() + "\t" +
                            typeInfo.getLocalTypeName() + "\t" +
                            typeInfo.getMinimumScale() + "\t" +
                            typeInfo.getMaximumScale()
            );

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
