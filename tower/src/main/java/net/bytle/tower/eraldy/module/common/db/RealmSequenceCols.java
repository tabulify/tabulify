package net.bytle.tower.eraldy.module.common.db;

import net.bytle.vertx.db.JdbcColumn;
import net.bytle.vertx.db.JdbcSchemaManager;

public enum RealmSequenceCols implements JdbcColumn {

  LAST_ID("sequence" + JdbcSchemaManager.COLUMN_PART_SEP + "last_id"),
  MODIFICATION_TIME( "sequence" + JdbcSchemaManager.COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX),
  CREATION_TIME_COLUMN ( "sequence" + JdbcSchemaManager.COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX),
  TABLE_NAME( "sequence" + JdbcSchemaManager.COLUMN_PART_SEP + "table_name"),
  REALM_ID( "sequence" + JdbcSchemaManager.COLUMN_PART_SEP + "realm_id");

  private final String columnName;

  RealmSequenceCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return columnName;
  }

}
