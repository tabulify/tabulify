package com.tabulify.mysql;

import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;

public class MySqlMetadata extends SqlConnectionMetadata {

  public MySqlMetadata(SqlConnection sqlConnection) {
    super(sqlConnection);
  }


  @Override
  public boolean isSchemaSeenAsCatalog() {
    return true;
  }
}
