package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataPath;
import net.bytle.db.jdbc.SqlDataStore;
import net.bytle.db.spi.DataPath;

public class SqliteDataPath extends AnsiDataPath implements DataPath {


  public static final String SCHEMA_NAME = "";
  private final SqliteDataStore sqliteDataStore;

  public SqliteDataPath(SqliteDataStore jdbcDataStore, String catalog, String schema, String name) {
    super(jdbcDataStore, catalog, schema, name);
    this.sqliteDataStore = jdbcDataStore;
  }

  @Override
  public AnsiDataPath getSchema() {
    return new SqliteDataPath(sqliteDataStore,"", SCHEMA_NAME,null);
  }

  @Override
  public SqlDataStore getDataStore() {
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

  @Override
  public SqliteDataPath getChild(String name) {
    if (super.getSchema().getName().equals(SCHEMA_NAME)){
      return new SqliteDataPath(sqliteDataStore,null,null,name);
    } else {
      throw  new RuntimeException("You can't ask a children from a table. You are asking a children from the table ("+this+")");
    }
  }
}
