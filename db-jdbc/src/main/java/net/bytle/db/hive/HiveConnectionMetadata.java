package net.bytle.db.hive;

import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlConnectionMetadata;

public class HiveConnectionMetadata extends SqlConnectionMetadata {
  public HiveConnectionMetadata(SqlConnection sqlConnection) {
    super(sqlConnection);
  }

  @Override
  public String getEscapeCharacter() {
    return "`";
  }

  @Override
  public Integer getMaxWriterConnection() {
    // The JDBCMetadata().getMaxConnections() method returns a Method Not Supported exception
    return 5;
  }

}
