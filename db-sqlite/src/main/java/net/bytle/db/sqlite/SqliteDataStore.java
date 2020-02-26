package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataSystem;

public class SqliteDataStore extends JdbcDataStore {

  public SqliteDataStore(String name, String url, JdbcDataSystem jdbcDataSystem) {
    super(name, url, jdbcDataSystem);
  }

}
