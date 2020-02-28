package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataPath;
import net.bytle.db.spi.DataPath;

public class SqliteDataPath extends AnsiDataPath implements DataPath {



  public SqliteDataPath(SqliteDataStore jdbcDataStore, String catalog, String schema, String name) {
    super(jdbcDataStore, catalog, schema, name);
    assert catalog==null:"Sqlite does not have the notion of catalog. The catalog should be null bit was ("+catalog+")";
    assert schema==null:"Sqlite does not have the notion of schema. The schema should be null bit was ("+schema+")";
  }

  public SqliteDataPath(SqliteDataStore sqliteDataStore, String query) {
    super(sqliteDataStore, query);
  }

  @Override
  public SqliteDataPath getSchema() {
    return null;
  }

  @Override
  public SqliteDataStore getDataStore() {
    return (SqliteDataStore) super.getDataStore();
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
    if (super.getName()==null){
      return this.getDataStore().getSqlDataPath(null,null,name);
    } else {
      throw  new RuntimeException("You can't ask a children from a table. You are asking a children from the table ("+this+")");
    }
  }


}
