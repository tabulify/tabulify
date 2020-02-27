package net.bytle.db.jdbc;

import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;
import net.bytle.log.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;

import static net.bytle.db.jdbc.SqlDataStore.DB_SQLITE;

/**
 * Static method for an ANSI database
 */
public class Ansis {


  private static final Log LOGGER = JdbcDataSystemLog.LOGGER_DB_JDBC;


  public static List<DataPath> getChildrenDataPath(AnsiDataPath jdbcDataPath) {
    return getDescendants(jdbcDataPath, null);
  }

  public static List<DataPath> getDescendants(AnsiDataPath jdbcDataPath, String tableNamePattern) {

    List<DataPath> jdbcDataPaths = new ArrayList<>();
    try {

      String schema = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
      String catalog = jdbcDataPath.getCatalog();
      String tableName = tableNamePattern;

      ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getTables(catalog, schema, tableName, null);
      while (tableResultSet.next()) {
        final String table_name = tableResultSet.getString("TABLE_NAME");
        final String schema_name = tableResultSet.getString("TABLE_SCHEM");
        final String cat_name = tableResultSet.getString("TABLE_CAT");
        final String type_name = tableResultSet.getString("TABLE_TYPE");
        AnsiDataPath childDataPath = jdbcDataPath.getDataStore().getDataPath(cat_name, schema_name, table_name)
          .setType(type_name);
        jdbcDataPaths.add(childDataPath);
      }


    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return jdbcDataPaths;

  }

  /**
   * @param jdbcDataPath
   * @return a list of data path that reference the primary key of the jdbcDataPath
   */
  public static List<DataPath> getReferencingDataPaths(AnsiDataPath jdbcDataPath) {

    List<DataPath> jdbcDataPaths = new ArrayList<>();
    try {

      String schema = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
      String catalog = jdbcDataPath.getCatalog();
      String tableName = jdbcDataPath.getName();

      ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getExportedKeys(catalog, schema, tableName);
      while (tableResultSet.next()) {
        final String table_name = tableResultSet.getString("FKTABLE_NAME");
        final String schema_name = tableResultSet.getString("FKTABLE_SCHEM");
        final String cat_name = tableResultSet.getString("FKTABLE_CAT");
        AnsiDataPath fkDataPath = AnsiDataPath.of(jdbcDataPath.getDataStore(), cat_name, schema_name, table_name);
        jdbcDataPaths.add(fkDataPath);
      }


    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return jdbcDataPaths;
  }











  public void printPrimaryKey(AnsiDataPath jdbcDataPath) {

    try (
      ResultSet resultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getPrimaryKeys(jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), jdbcDataPath.getName())
    ) {
      while (resultSet.next()) {
        System.out.println("Primary Key Column: " + resultSet.getString("COLUMN_NAME"));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  public void printUniqueKey(AnsiDataPath jdbcDataPath) {

    try (
      ResultSet resultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getIndexInfo(jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), jdbcDataPath.getName(), true, false)
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
  public static void printDatabaseInformation(SqlDataStore jdbcDataStore) {

    System.out.println("Information about the database (" + jdbcDataStore.getName() + "):");

    System.out.println();
    System.out.println("Driver Information:");
    DatabaseMetaData databaseMetadata = null;
    final Connection currentConnection = jdbcDataStore.getCurrentConnection();
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
        url = new URI(jdbcDataStore.getConnectionString());
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
  public static void printDataTypeInformation(SqlDataStore jdbcDataStore) {

    Set<SqlDataType> sqlDataTypes = jdbcDataStore.getSqlDataTypes();

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

    for (SqlDataType typeInfo : sqlDataTypes) {
      System.out.println(
        typeInfo.getTypeCode() + "\t" +
          typeInfo.getTypeNames() + "\t" +
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

  public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {
    /**
     * TODO: move that outside of the core
     * for now a hack
     * because Sqlite does not support alter table drop foreign keys
     */
    SqlDataStore dataStore = (SqlDataStore) foreignKeyDef.getTableDef().getDataPath().getDataStore();
    if (!dataStore.getProductName().equals(DB_SQLITE)) {
      AnsiDataPath jdbcDataPath = (AnsiDataPath) foreignKeyDef.getTableDef().getDataPath();
      String dropStatement = "alter table " + JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath) + " drop constraint " + foreignKeyDef.getName();
      try {

        Statement statement = dataStore.getCurrentConnection().createStatement();
        statement.execute(dropStatement);
        statement.close();

        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Foreign Key (" + foreignKeyDef.getName() + ") deleted from the table (" + jdbcDataPath.toString() + ")");

      } catch (SQLException e) {

        System.err.println(dropStatement);
        throw new RuntimeException(e);

      }
    }
  }


}
