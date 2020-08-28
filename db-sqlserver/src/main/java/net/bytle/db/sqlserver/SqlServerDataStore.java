package net.bytle.db.sqlserver;

import net.bytle.db.jdbc.SqlDataStore;

/**
 * Created by gerard on 28-11-2015.
 * <p>
 * Tracing
 * https://docs.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation?view=sql-server-2017
 * Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
 * logger.setLevel(Level.FINE);
 */
public class SqlServerDataStore extends SqlDataStore {


  public SqlServerDataStore(String name, String url) {
    super(name, url);
  }


  @Override
  public Integer getMaxWriterConnection() {
    return 100;
  }

  @Override
  public SqlServerSystem getDataSystem() {
    return new SqlServerSystem(this);
  }
}
