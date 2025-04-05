package net.bytle.db.mysql;

import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlConnectionMetadata;

public class MySqlMetadata extends SqlConnectionMetadata {

  public MySqlMetadata(SqlConnection sqlConnection) {
    super(sqlConnection);
  }


  @Override
  public boolean isSchemaSeenAsCatalog() {
    return true;
  }
}
