package net.bytle.db.jdbc;

import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;

public  class JdbcDataDef extends TableDef implements RelationDef {

  private final JdbcDataPath jdbcDataPath;

  public JdbcDataDef(JdbcDataPath dataPath) {
    super(dataPath);
    this.jdbcDataPath = dataPath;
  }

  @Override
  public JdbcDataPath getDataPath(){
    return jdbcDataPath;
  }



}
