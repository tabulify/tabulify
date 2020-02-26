package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataStore;
import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.spi.DataPath;

public class SqliteDataPath extends JdbcDataPath implements DataPath {


  private final SqliteDataStore sqliteDataStore;
  private SqliteDataDef sqliteDataDef;

  public SqliteDataPath(SqliteDataStore jdbcDataStore, String catalog, String schema, String name) {
    super(jdbcDataStore, catalog, schema, name);
    this.sqliteDataStore = jdbcDataStore;
  }

  @Override
  public JdbcDataPath getSchema() {
    return new SqliteDataPath(sqliteDataStore,"","",null);
  }

  @Override
  public AnsiDataStore getDataStore() {
    return super.getDataStore();
  }

  @Override
  public SqliteDataDef getDataDef() {
    if (sqliteDataDef == null){
      sqliteDataDef = new SqliteDataDef(this);
    }
    return sqliteDataDef;
  }
}
