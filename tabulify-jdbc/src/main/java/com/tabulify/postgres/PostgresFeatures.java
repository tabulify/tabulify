package com.tabulify.postgres;

import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;

/**
 * Not used for now, just to add the below link:
 * <p></p>
 * <a href="https://www.postgresql.org/docs/current/features.html">...</a>
 *
 *
 */
public class PostgresFeatures extends SqlConnectionMetadata {

  public PostgresFeatures(SqlConnection sqlConnection) {
    super(sqlConnection);
  }

  /**
   * Postgres supports it but the catalog name is always postgres
   * so no added value.
   */
  @Override
  public boolean supportsCatalogsInSqlStatementPath() {
    return false;
  }

  @Override
  public Integer getMaxNamesInPath() {
    return 2;
  }

}
