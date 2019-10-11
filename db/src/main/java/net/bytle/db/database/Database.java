package net.bytle.db.database;

import net.bytle.db.DatabasesStore;
import net.bytle.db.DbDefaultValue;
import net.bytle.db.DbLoggers;
import net.bytle.db.connection.URIExtended;
import net.bytle.db.database.Hana.SqlDatabaseIHana;
import net.bytle.db.database.Hive.SqlDatabaseIHive;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.database.Oracle.SqlDatabaseIOracle;
import net.bytle.db.database.SqlServer.SqlDatabaseISqlServer;
import net.bytle.db.model.*;
import net.bytle.cli.Log;
import net.bytle.regexp.Globs;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A wrapper around a connection !
 */
public class Database implements AutoCloseable, Comparable<Database> {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    public static final String DB_ORACLE = "Oracle";
    public static final String DB_HANA = "HDB";
    public static final String DB_SQL_SERVER = "Microsoft SQL Server";
    public static final String DB_SQLITE = "SQLite";
    public static final String DB_HIVE = "Apache Hive";
    public static final String DB_ANSI = "Ansi";

    // The map that will contain the driver data type
    private Map<Integer, DataTypeDriver> dataTypeInfoMap;

    // The map that contains the data type
    private Map<Integer, DataTypeDriver> dataTypeDriverMap = new HashMap<>();

    private DatabaseMetaData databaseMetadata;

    private SqlDatabaseI sqlDatabaseI;

    // The database name
    private final String databaseName;

    private Connection currentConnection;

    private String databaseProductName;

    // A cache object
    // integer is data type id
    private Map<Integer, DataType> dataTypeMap = new HashMap<>();

    // An object that we use to forward all build function (ie getTableOf, getSchema)
    // It manage also the object cache
    // It is located in the schema package
    private DbObjectBuilder objectBuilder = new DbObjectBuilder(this);

    // Jdbc Url
    private String url;

    // Jdbc Driver
    private String driver;
    private String user;
    private String postStatement;
    private String password;
    private DatabasesStore databaseStore;


    Database(String databaseName) {

        this.databaseName = databaseName;

    }


    /**
     * TODO: Move that in a SQL manager
     * The databaseName of a table in a SQL statement
     */
    public String getStatementTableName(String objectName) {

        String identifierQuoteString = "\"";
        try {
            final Connection currentConnection = this.getCurrentConnection();
            if (currentConnection!=null) {
                identifierQuoteString = currentConnection.getMetaData().getIdentifierQuoteString();
            }
        } catch (SQLException e) {
            LOGGER.warning("The database "+this+" throw an error when retrieving the quoted string identifier."+e.getMessage());
        }
        String normativeObjectName = identifierQuoteString+objectName+identifierQuoteString;
        if (this.getSqlDatabase() != null) {
            String objectNameExtension = this.getSqlDatabase().getNormativeSchemaObjectName(objectName);
            if (objectNameExtension != null) {
                normativeObjectName = objectNameExtension;
            }
        }
        return normativeObjectName;

        // Default
//        Pattern pattern = Pattern.compile("[0-9A-Za-z_]");
//        StringBuilder newName = new StringBuilder();
//        Boolean previousCharWasSeparator = false;
//        char separatorChar = "_".toCharArray()[0];
//        for (char c : objectName.trim().toCharArray()) {
//            Matcher matcher = pattern.matcher(String.valueOf(c));
//            if (matcher.find()) {
//                if (c == separatorChar) {
//                    if (!previousCharWasSeparator) {
//                        newName.append(c);
//                    }
//                    previousCharWasSeparator = true;
//                } else {
//                    newName.append(c);
//                    previousCharWasSeparator = false;
//                }
//            } else {
//                if (!previousCharWasSeparator) {
//                    newName.append(separatorChar);
//                    previousCharWasSeparator = true;
//                }
//            }
//        }
//        String newNameString = newName.toString();
//        if (newNameString.length() > 30) {
//            newNameString = newNameString.substring(0, 30);
//        }
//
//        return newNameString.toUpperCase();


    }


    /**
     * Return the data type (info) from the driver
     *
     * @param typeCode the type code
     * @return
     */
    private DataTypeDriver getDataTypeDriver(Integer typeCode) {

        if (this.getDatabaseMetadata() == null) {
            return null;
        } else {
            if (dataTypeInfoMap == null) {
                dataTypeInfoMap = new HashMap<>();
                ResultSet typeInfoResultSet;
                try {
                    typeInfoResultSet = this.getDatabaseMetadata().getTypeInfo();
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
    }

    private DatabaseMetaData getDatabaseMetadata() {

        if (databaseMetadata == null) {
            if (this.getCurrentConnection() != null) {
                try {
                    databaseMetadata = this.getCurrentConnection().getMetaData();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return databaseMetadata;


    }

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

        System.out.println("Information about the database (" + this.getDatabaseName() + "):");

        System.out.println();
        System.out.println("Driver Information:");
        DatabaseMetaData databaseMetadata = this.getDatabaseMetadata();
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
            System.out.println("Catalog: " + this.currentConnection.getCatalog());
            String schema;
            if (databaseMetadata.getJDBCMajorVersion() >= 7) {
                schema = this.currentConnection.getSchema();
            } else {
                schema = "The JDBC Driver doesn't have this information.";
            }
            System.out.println("Schema: " + schema);
            System.out.println("Schema Current Connection: " + this.getCurrentSchema());
            System.out.println("Client Info");
            Properties clientInfos = this.currentConnection.getClientInfo();
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
                url = new URI(this.url);
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

    /**
     * Utility that transforms a resultSetMetadata in a list of columnsMetadata
     *
     * @param resultSetMetaData
     * @return a list of ColumnMetadata that can be use in DDL getter function such as {@link #getInsertStatement(TableDef)}
     */


    /**
     * Return the current Connection
     *
     * @return the current connection or null (if no URL)
     * <p>
     * The current connection is the first connection created
     */
    public Connection getCurrentConnection() {

        if (this.url == null) {
            return null;
        }

        if (this.currentConnection == null) {
            this.currentConnection = getNewConnection(Databases.MODULE_NAME);
        }
        try {
            if (this.currentConnection.isClosed()) {

                // With the database id being the database name, this is not true anymore ?
                // throw new RuntimeException("The connection was closed ! We cannot reopen it otherwise the object id will not be the same anymore");
                LOGGER.severe("The database connection was closed ! We reopen it.");
                this.currentConnection = getNewConnection("main");

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return currentConnection;

    }

    /**
     * Return the database Name (seen as a database ID)
     *
     * @return the database Product Name (for now given by the driver)
     * <p>
     * You can test it with the final static variable
     */
    public String getDatabaseProductName() {
        try {

            if (this.databaseProductName != null) {
                return this.databaseProductName;
            }

            if (this.url != null) {
                this.databaseProductName = getCurrentConnection().getMetaData().getDatabaseProductName();
                return this.databaseProductName;
            }

            return DB_ANSI;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
                maxWriterConnection = this.getDatabaseMetadata().getMaxConnections();
                // 0 writer is not really possible
                if (maxWriterConnection == 0) {
                    return 1;
                } else {
                    return maxWriterConnection;
                }
            } catch (SQLException e) {
                LOGGER.severe("Tip: The getMaxConnections is may be not supported on the JDBC driver. Adding it to the extension will resolve this problem.");
                throw new RuntimeException(e);
            }
        }

    }


    /**
     * If the URL is null, it will return NULL
     *
     * @return a sql connection
     */
    public synchronized Connection getNewConnection(String appName) {

        if (this.url == null) {
            return null;
        }

        if (this.driver == null) {
            URIExtended uriExtended = new URIExtended(this.url);
            this.driver = uriExtended.getDriver();
        }
        if (this.driver != null) {
            try {
                Class.forName(this.driver);
                LOGGER.info("Driver " + this.driver + " loaded");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("The driver Class (" + this.driver + ") could not be loaded. An error occurs: " + e.getMessage() + ". May be that the driver is not on the path ?", e);
            }
        }

        Connection connection;
        LOGGER.info("Trying to connect to the connection (" + this.url + ")");
        try {

            // connection = DriverManager.getConnection(this.url, this.user, this.password);
            Properties connectionProperties = new Properties();
            // Sql Server
            // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
            //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
            connectionProperties.put("applicationName", DbDefaultValue.LIBRARY_NAME + " " + appName);
            if (this.user != null) {
                connectionProperties.put("user", this.user);
                if (this.password != null) {
                    connectionProperties.put("password", this.password);
                }
            }
            connection = DriverManager.getConnection(this.url, connectionProperties);

        } catch (SQLException e) {
            String msg = "Unable to connect to the database with the following URL (" + this.url + "). Error: " + e.getMessage();
            LOGGER.severe(msg);
            throw new RuntimeException(e);
        }


        // Post Connection statement (such as alter session set current_schema)
        if (this.postStatement != null) {
            try (CallableStatement callableStatement = connection.prepareCall(this.postStatement)) {

                callableStatement.execute();

            } catch (SQLException e) {
                throw new RuntimeException("Post Connection error occurs: " + e.getMessage(), e);
            }
        }
        LOGGER.info("Connected !");
        return connection;

    }

    @Override
    public void close() {

        if (this.getCurrentConnection() != null) {
            try {
                this.currentConnection.close();
                LOGGER.info("The connection (" + this.url + ") was closed.");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }


    /**
     * For database that does not support multiple schema (Sqlite for instance), you will get a schema without databaseName
     *
     * @param schema
     * @return
     */
    public SchemaDef getSchema(String schema) {


        return new SchemaDef(this)
                .name(schema);


    }


    /**
     * Create a merge statement to load data in a table
     * TODO: merge columns can be found at: {@link DatabaseMetaData#getBestRowIdentifier(String, String, String, int, boolean)}
     *
     * @param tableDef
     * @param mergeColumnPositions
     * @return a merge statement that is used by the loader
     */
    public String getMergeStatement(TableDef tableDef, List<Integer> mergeColumnPositions) {

        String sql = "INSERT OR REPLACE INTO " + tableDef.getName() + "(";

        // Columns
        String columnsName = "TODO";
        // Level 8 syntax
        //        tableDef.getColumnDefs().stream()
        //                .map(ColumnDef::getColumnName)
        //                .collect(Collectors.joining(", "));

        sql += columnsName + ") values (";

        for (int i = 0; i < tableDef.getColumnDefs().size(); i++) {
            sql += "?";
            if (!(i >= tableDef.getColumnDefs().size() - 1)) {
                sql += ",";
            }
        }
        sql += ")";

        return sql;

    }


    public SqlDatabaseI getSqlDatabase() {

        if (sqlDatabaseI == null) {
            if (this.getDatabaseProductName() != null) {

                switch (this.getDatabaseProductName()) {
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
                        sqlDatabaseI = SqlDatabases.getSqlDatabase(url);
                        break;
                    case DB_HIVE:
                        sqlDatabaseI = new SqlDatabaseIHive(this);
                        break;
                }
            }
        }
        return sqlDatabaseI;
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
                    .DriverDataType(dataTypeDriver)
                    .JdbcDataType(dataTypeJdbc)
                    .build();

            dataTypeMap.put(typeCode, dataType);
        }

        return dataType;

    }

    /**
     * try to build the table from the database connection
     * If not found, you will get a empty {@Link TableDef} object
     *
     * @param tableName
     * @return
     */
    public TableDef getTable(String tableName) {

        String schema = this.getCurrentSchema().getName();
        return getTable(tableName, schema);

    }

    /**
     * This function return a tableDef from the database
     * <p>
     * If the schemaNameTest is null, it will default to the connection schema
     *
     * @param tableName
     * @param schemaName
     * @return
     */
    public TableDef getTable(String tableName, String schemaName) {

        if (schemaName == null) {
            schemaName = this.getCurrentSchema().getName();
        }
        return objectBuilder.getTableDef(tableName, schemaName);

    }


    public SchemaDef getCurrentSchema() {
        try {

            String schema;
            switch (this.getDatabaseProductName()) {
                case DB_ANSI:
                    schema = null;
                    break;
                case DB_ORACLE:
                    // The function getSchema with the Oracle Driver throws an error (abstract ...)
                    schema = this.getCurrentConnection().getMetaData().getUserName();
                    break;
                default:
                    schema = this.getCurrentConnection().getSchema();
                    break;
            }

            return getSchema(schema);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public DbObjectBuilder getObjectBuilder() {
        return objectBuilder;
    }


    /**
     * Create the table tableName with the column of the sourceDef
     *
     * @param tableName
     * @param sourceDef
     * @param schemaName
     * @return
     */
    public TableDef getTable(String tableName, RelationDef sourceDef, String schemaName) {

        return objectBuilder.getTableDef(sourceDef, tableName, schemaName);

    }


    /**
     * @param pattern
     * @return the list of schema for this database
     */
    public List<SchemaDef> getSchemas(String pattern) {


        List<SchemaDef> schemaDefList = this.getObjectBuilder().buildSchemas(this);
        if (pattern == null) {
            return schemaDefList;
        } else {
            String schemaPatternName = Globs.toRegexPattern(pattern);
            return schemaDefList
                    .stream()
                    .filter(s -> s.getName().matches(schemaPatternName))
                    .collect(Collectors.toList());
        }

    }

    public Database setDriver(String jdbcDriver) {
        this.driver = jdbcDriver;
        return this;
    }


    public String getDatabaseName() {
        return databaseName;
    }

    public String getUrl() {
        return this.url;
    }

    public Database setUrl(String jdbcUrl) {
        if (this.url == null || this.url.equals(jdbcUrl)) {

            this.url = jdbcUrl;

        } else {

            throw new RuntimeException("The URL cannot be changed. It has already the value (" + this.url + ") and cannot be set to (" + jdbcUrl + ")");

        }
        return this;
    }

    @Override
    public String toString() {
        return databaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Database database = (Database) o;
        return Objects.equals(databaseName, database.databaseName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(databaseName);
    }

    public QueryDef getQuery(String query) {
        return objectBuilder.getQueryDef(query);
    }

    public List<SchemaDef> getSchemas() {
        return getSchemas(null);
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

    public int getDatabaseMajorVersion() {
        try {
            return this.currentConnection.getMetaData().getDatabaseMajorVersion();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getDatabaseMinorVersion() {
        try {
            return this.currentConnection.getMetaData().getDatabaseMinorVersion();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     *
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
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
     *
     * @return the scheme of the data store
     *   * file
     *   * ...
     *
     */
    public String getScheme() {
        if(url==null){
            return DatabasesStore.LOCAL_FILE_SYSTEM;
        } else {
            return getUrl().substring(0,getUrl().indexOf(":"));
        }
    }

}
