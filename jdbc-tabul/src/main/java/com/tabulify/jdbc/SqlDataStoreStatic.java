package com.tabulify.jdbc;

import com.tabulify.model.SqlDataType;
import com.tabulify.type.JdbcUri;

import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

/**
 * Static method for a datastore
 * that should be elsewhere
 */
public class SqlDataStoreStatic {


  /**
   * Todo: Add {@link DatabaseMetaData#getClientInfoProperties()}
   */
  public static void printDatabaseInformation(SqlConnection jdbcDataStore) {


    System.out.println();
    System.out.println("Driver Information:");
    DatabaseMetaData databaseMetadata;
    final Connection currentConnection = jdbcDataStore.getCurrentJdbcConnection();
    try {

      databaseMetadata = currentConnection.getMetaData();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    try {


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
      if (clientInfos != null && !clientInfos.isEmpty()) {
        for (String key : clientInfos.stringPropertyNames()) {
          System.out.println("  * (" + key + ") = (" + clientInfos.getProperty(key) + ")");
        }
      } else {
        System.out.println("   * No client infos");
      }

      System.out.println();
      URI url = jdbcDataStore.getUri().toUri();
      JdbcUri jdbcUri = new JdbcUri(url);
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
      System.out.println("Driver: " + jdbcUri.getDriver());
      System.out.println("Server: " + jdbcUri.getSqlScheme());

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Print data type given by the driver
   */
  public static void printDataTypeInformation(SqlConnection jdbcDataStore) {

    Set<SqlDataType<?>> sqlDataTypes = jdbcDataStore.getSqlDataTypes();

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
        typeInfo.getVendorTypeNumber() + "\t" +
          typeInfo.toKeyNormalizer() + "\t" +
          typeInfo.getMaxPrecision() + "\t" +
          typeInfo.getLiteralPrefix() + "\t" +
          typeInfo.getLiteralSuffix() + "\t" +
          typeInfo.getCreateParams() + "\t" +
          typeInfo.getNullable() + "+\t" +
          typeInfo.getCaseSensitive() + "\t" +
          typeInfo.getSearchable() + "\t" +
          typeInfo.getUnsignedAttribute() + "\t" +
          typeInfo.getIsFixedPrecisionScale() + "\t" +
          typeInfo.getLocalTypeName() + "\t" +
          typeInfo.getMinimumScale() + "\t" +
          typeInfo.getMaximumScale()
      );

    }


  }


}
