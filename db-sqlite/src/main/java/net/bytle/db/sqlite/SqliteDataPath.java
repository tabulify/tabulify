package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.spi.DataPath;

public class SqliteDataPath extends JdbcDataPath implements DataPath {


  private final SqliteDataStore sqliteDataStore;

  public SqliteDataPath(SqliteDataStore jdbcDataStore, String catalog, String schema, String name) {
    super(jdbcDataStore, catalog, schema, name);
    this.sqliteDataStore = jdbcDataStore;
  }

  @Override
  public JdbcDataPath getSchema() {
    return new SqliteDataPath(sqliteDataStore,"","",null);
  }

  @Override
  public JdbcDataStore getDataStore() {
    return super.getDataStore();
  }

}
