package net.bytle.tower.eraldy.objectProvider;

import net.bytle.vertx.db.JdbcSchemaManager;
import net.bytle.vertx.db.JdbcTableColumn;

import static net.bytle.tower.eraldy.objectProvider.RealmProvider.TABLE_PREFIX;
import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public enum RealmCols implements JdbcTableColumn {

  ID(TABLE_PREFIX + COLUMN_PART_SEP + "id"),

  HANDLE(TABLE_PREFIX + COLUMN_PART_SEP + "handle"),
  ORGA_ID(TABLE_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN),
  OWNER_ID( TABLE_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + UserProvider.ID_COLUMN),

  CREATION_TIME(TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX),
  MODIFICATION_TIME(TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX),


  USER_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "user" + COLUMN_PART_SEP + "count"),
  LIST_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "list" + COLUMN_PART_SEP + "count"),
  APP_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "app" + COLUMN_PART_SEP + "count"),
  NAME(TABLE_PREFIX + COLUMN_PART_SEP + "name");

  private final String columnName ;

  RealmCols(String s) {
    this.columnName = s;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }

  @Override
  public String toString() {
    return columnName;
  }

}
