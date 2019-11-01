package net.bytle.db.jdbc;

import net.bytle.db.jdbc.spi.DataTypeDriver;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.regexp.Globs;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Static method
 *
 * Note for later:
 * String regexpPattern = Globs.toRegexPattern(globPattern);
 */
public class Jdbcs {

    /**
     * When this method is called, it's because the schema was not yet built
     *
     * @param jdbcDataPath - representing a schema path (ie a schema)
     * @return
     */
    public static List<JdbcDataPath> getTables(JdbcDataPath jdbcDataPath) {

        List<JdbcDataPath> jdbcDataPaths = new ArrayList<>();


        String[] types = {"TABLE"};

        try {
            ResultSet tableResultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getTables(jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), null, types);
            List<String> objectNames = new ArrayList<>();
            while (tableResultSet.next()) {

                objectNames.add(tableResultSet.getString("TABLE_NAME"));

            }
            tableResultSet.close();

            // getRelationDef make also used of the getDataPaths
            // and it seems that there is a cache because the result was closed
            // We do then a second loop
            for (String objectName : objectNames) {
                jdbcDataPaths.add(JdbcDataPath.of(jdbcDataPath.getDataSystem(), jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), objectName));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Collections.sort(jdbcDataPaths);
        return jdbcDataPaths;

    }

    /**
     * @param jdbcDataPath - a catalog or a schema pattern (should be a uri...)
     * @return the list of schema for this database
     */
    public static List<JdbcDataPath> getSchemas(JdbcDataPath jdbcDataPath) {

        List<JdbcDataPath> jdbcDataPaths = new ArrayList<>();


        try {

            // Always NULL
            // because otherwise it's not a pattern but
            // it must match the schema name
            // We build all schemas then
            final String schemaPattern = null;
            ResultSet schemaResultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getSchemas(jdbcDataPath.getCatalog(), schemaPattern);

            // Sqlite Driver return a NULL resultSet
            // because SQLite does not support schema ?
            if (schemaResultSet != null) {
                while (schemaResultSet.next()) {

                    jdbcDataPaths.add(JdbcDataPath.of(jdbcDataPath.getDataSystem(),jdbcDataPath.getCatalog(),schemaResultSet.getString("TABLE_SCHEM"),null));

                }
                schemaResultSet.close();
            }

        } catch (java.sql.SQLFeatureNotSupportedException e) {

            JdbcDataSystemLog.LOGGER_DB_JDBC.warning("Schemas are not supported on this database.");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return jdbcDataPaths;
    }


    /**
     * Retrieve the relationship (ie foreigns key and external key) of tables
     *
     * @param jdbcDataPath - the name of a table or a glob pattern
     * @return
     */
    public List<ForeignKeyDef> getForeignKeys(JdbcDataPath jdbcDataPath) {
        Set<ForeignKeyDef> foreignKeys = new HashSet<>();
        String regexpPattern = Globs.toRegexPattern(jdbcDataPath.getName());
        for (JdbcDataPath dataPath : getTables(jdbcDataPath.getSchema())) {
            if (dataPath.getName().matches(regexpPattern)) {
                foreignKeys.addAll(dataPath.getDataDef().getForeignKeys());
            }
            for (ForeignKeyDef foreignKeyDef: dataPath.getDataDef().getForeignKeys()){
                if (foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath().getName().matches(regexpPattern)){
                    foreignKeys.add(foreignKeyDef);
                }
            }
        }
        return new ArrayList<>(foreignKeys);
    }

    public void printPrimaryKey(JdbcDataPath jdbcDataPath) {

        try (
                ResultSet resultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getPrimaryKeys(jdbcDataPath.getCatalog(),jdbcDataPath.getSchema().getName() ,jdbcDataPath.getName())
        ) {
            while (resultSet.next()) {
                System.out.println("Primary Key Column: " + resultSet.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void printUniqueKey(JdbcDataPath jdbcDataPath) {

        try (
                ResultSet resultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getIndexInfo(jdbcDataPath.getCatalog(),jdbcDataPath.getSchema().getName() ,jdbcDataPath.getName(), true, false)
        ) {
            while (resultSet.next()) {
                System.out.println("Unique Key Column: " + resultSet.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Todo: Add {@link DatabaseMetaData#getClientInfoProperties()}
     */
    public static  void printDatabaseInformation(JdbcDataSystem jdbcDataSystem) {

        System.out.println("Information about the database (" + jdbcDataSystem.getDatabase().getDatabaseName() + "):");

        System.out.println();
        System.out.println("Driver Information:");
        DatabaseMetaData databaseMetadata = null;
        final Connection currentConnection = jdbcDataSystem.getCurrentConnection();
        try {

            databaseMetadata = currentConnection.getMetaData();
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
            System.out.println("Catalog: " + currentConnection.getCatalog());
            String schema;
            if (databaseMetadata.getJDBCMajorVersion() >= 7) {
                schema = currentConnection.getSchema();
            } else {
                schema = "The JDBC Driver doesn't have this information.";
            }
            System.out.println("Schema: " + schema);
            System.out.println("Schema Current Connection: " + currentConnection.getSchema());
            System.out.println("Client Info");
            Properties clientInfos = currentConnection.getClientInfo();
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
                url = new URI(jdbcDataSystem.getDatabase().getUrl());
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
    public static void printDataTypeInformation(Connection connection) {

        List<DataTypeDriver> dataTypeDrivers = new ArrayList<>(getDataTypeDriver(connection).values());

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

    public static Map<Integer, DataTypeDriver> getDataTypeDriver(Connection connection){

        Map<Integer, DataTypeDriver> dataTypeInfoMap = new HashMap<>();
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
        return dataTypeInfoMap;
    }

    /**
     * Return an object to be set in a prepared statement (for instance)
     * Example: if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as a
     * oracle.sql.BINARY_DOUBLE
     *
     * @param targetConnection the target connection
     * @param targetColumnType the target column type
     * @param sourceObject     the java object to be loaded
     * @return
     */
    public static Object castLoadObjectIfNecessary(Connection targetConnection, int targetColumnType, Object sourceObject) {

        String databaseProductName;
        try {
            databaseProductName = targetConnection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // See oracle
        return sourceObject;

    }



}
