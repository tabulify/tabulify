package com.tabulify.jdbc;

import com.tabulify.connection.ConnectionMetadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * A metadata object for the datastore that gives information
 * about the features
 *
 * You must see this as:
 *   * the in-memory representation of properties that are used by the {@link SqlDataSystem}
 *   * a wrapper around {@link DatabaseMetaData}
 *
 * Some page about Features
 *   * https://en.wikipedia.org/wiki/SQL_compliance
 *
 *
 */
public class SqlConnectionMetadata extends ConnectionMetadata {


  private final DatabaseMetaData metadata;



  public SqlConnectionMetadata(SqlConnection sqlConnection) {
    super(sqlConnection);

    try {
      this.metadata = sqlConnection.getCurrentConnection().getMetaData();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }



  }

  /**
   * Return the number of name in the {@link SqlDataPath path } that supports this datastore
   * if the datastore does not support a catalog, it will be 2
   * if the datastore does not support a schema, it will be 1
   * if the datastore supports a catalog and a schema, it will be 3
   *
   */
  @Override
  public Integer getMaxNamesInPath() {

    try {
      int maxNames = 1;
      if (metadata.supportsSchemasInDataManipulation() || metadata.supportsSchemasInDataManipulation()) {
        maxNames++;
      }
      if (this.supportsCatalogsInSqlStatementPath()) {
        maxNames++;
      }
      return maxNames;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Database such as MySql does not support schemas
   * but support catalogs.
   *
   * They support only one name namespace.
   *
   * And the driver returns the name as `catalog`
   * not `schema` (Schema is always null)
   *
   * This function permits defining if the driver
   * has this behavior
   *
   *
   */
  public boolean isSchemaSeenAsCatalog(){
    return false;
  }



  /**
   * Do the datastore supports the fact to
   * add the catalog name in the {@link SqlConnectionResourcePath#toSqlStatementPath()}}
   * sql statement path
   * <p></p>
   * For instance, Postgres does not need it, but
   * it supports it for ISO conformance reason
   *
   */
  public boolean supportsCatalogsInSqlStatementPath() {

    try {
      return this.metadata.isCatalogAtStart() || this.metadata.supportsCatalogsInTableDefinitions() || this.metadata.supportsCatalogsInDataManipulation();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * @return the escape character in SQL pattern
   * used in {@link net.bytle.regexp.Glob#toSqlPattern(String)}
   */
  public String getEscapeCharacter() {
    try {
      return metadata.getSearchStringEscape();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the quote for identifier such as table, column name in a sql
   */
  String getIdentifierQuote() {
    String identifierQuoteString = "\"";
    try {
        identifierQuoteString = metadata.getIdentifierQuoteString();
    } catch (SQLException e) {
      SqlLog.LOGGER_DB_JDBC.warning("The database (" + this + ") throw an error when retrieving the quoted string identifier." + e.getMessage());
    }
    return identifierQuoteString;
  }


  /**
   * Does the datastores support sql parameters
   */
  public boolean supportsSqlParameters() {
    // Name parameters is much more when you give parameters by name than by index
    // connection.getMetaData().supp .supportsNamedParameters();
    return true;
  }

  /**
   * @return the number of concurrent writer connection
   */
  @Override
  public Integer getMaxWriterConnection() {
    try {
      int maxWriterConnection = ((SqlConnection) this.getConnection()).getCurrentConnection().getMetaData().getMaxConnections();
      // 0 writer is not really possible
      if (maxWriterConnection == 0) {
        return 1;
      } else {
        return maxWriterConnection;
      }
    } catch (SQLException e) {
      SqlLog.LOGGER_DB_JDBC.severe("Tip: The getMaxConnections is may be not supported on the JDBC driver. Adding it to the extension will resolve this problem.");
      throw new RuntimeException(e);
    }
  }

  /**
   * An utility function to return a {@link DatabaseMetaData}
   * without the exception
   *
   */
  public DatabaseMetaData getDatabaseMetaData() {
    try {
      return ((SqlConnection) this.getConnection()).getCurrentConnection().getMetaData();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  public String getDatabaseProductVersion() {
    try {
      return this.getDatabaseMetaData().getDatabaseProductVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDatabaseProductName() {
    try {
      return this.getDatabaseMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Integer getDatabaseMajorVersion() {
    try {
      return this.getDatabaseMetaData().getDatabaseMajorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Integer getDatabaseMinorVersion() {
    try {
      return this.getDatabaseMetaData().getDatabaseMinorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Integer getJdbcMinorVersion() {
    try {
      return this.getDatabaseMetaData().getJDBCMinorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Integer getJdbcMajorVersion() {
    try {
      return this.getDatabaseMetaData().getJDBCMajorVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDriverVersion() {
    try {
      return this.getDatabaseMetaData().getDriverVersion();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDriverName() {
    try {
      return this.getDatabaseMetaData().getDriverName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Boolean getSupportBatchUpdates() {
    try {
      return this.getDatabaseMetaData().supportsBatchUpdates();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Boolean getSupportNamedParameters() {
    try {
      return this.getDatabaseMetaData().supportsNamedParameters();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  public Boolean isQuotingEnabled() {
    return this.getConnection().getAttribute(SqlConnectionAttributeEnum.NAME_QUOTING_ENABLED).getValueOrDefaultCastAsSafe(Boolean.class);
  }
}
