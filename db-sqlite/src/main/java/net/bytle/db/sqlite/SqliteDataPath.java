package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;

public class SqliteDataPath extends JdbcDataPath {

  public SqliteDataPath(JdbcDataStore jdbcDataStore, String query) {
    super(jdbcDataStore, query);
  }

}
