package com.tabulify.postgres;

import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;

/**
 * Not used for now, just to add the below link:
 *
 * https://www.postgresql.org/docs/current/features.html
 *
 *
 */
public class PostgresFeatures extends SqlConnectionMetadata {

  public PostgresFeatures(SqlConnection sqlConnection) {
    super(sqlConnection);
  }

}
