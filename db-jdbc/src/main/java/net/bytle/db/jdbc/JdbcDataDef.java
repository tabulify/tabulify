package net.bytle.db.jdbc;

import net.bytle.db.model.RelationDef;

public interface JdbcDataDef extends RelationDef {

  @Override
  JdbcDataPath getDataPath();


}
