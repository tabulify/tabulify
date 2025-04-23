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

  @Override
  public boolean supportsCatalogsInSqlStatementPath() {
    return false;
  }

  @Override
  public Integer getMaxNamesInPath() {
    return 2;
  }

}
