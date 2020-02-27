package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataPath;
import net.bytle.db.jdbc.AnsiDataStore;
import net.bytle.db.spi.DataPath;

public class SqliteDataPath extends AnsiDataPath implements DataPath {


  private final SqliteDataStore sqliteDataStore;
  private SqliteDataDef sqliteDataDef;

  public SqliteDataPath(SqliteDataStore jdbcDataStore, String catalog, String schema, String name) {
    super(jdbcDataStore, catalog, schema, name);
    this.sqliteDataStore = jdbcDataStore;
  }

  @Override
  public AnsiDataPath getSchema() {
    return new SqliteDataPath(sqliteDataStore,"","",null);
  }

  @Override
  public AnsiDataStore getDataStore() {
    return super.getDataStore();
  }

  @Override
  public SqliteDataDef getOrCreateDataDef() {
    if (relationDef == null){
      relationDef = new SqliteDataDef(this,true);
    }
    return (SqliteDataDef) relationDef;
  }

  @Override
  public SqliteDataDef createDataDef() {
    if (relationDef == null){
      relationDef = new SqliteDataDef(this,false);
    }
    return (SqliteDataDef) relationDef;
  }
}
