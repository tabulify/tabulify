package net.bytle.tower.eraldy.module.realm.db;

import net.bytle.tower.eraldy.module.organization.db.OrganizationProvider;
import net.bytle.vertx.db.JdbcColumn;
import net.bytle.vertx.db.JdbcSchemaManager;

import static net.bytle.tower.eraldy.module.realm.db.RealmProvider.TABLE_PREFIX;
import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public enum RealmCols implements JdbcColumn {

  ID(TABLE_PREFIX + COLUMN_PART_SEP + "id"),
  NAME(TABLE_PREFIX + COLUMN_PART_SEP + "name"),

  HANDLE(TABLE_PREFIX + COLUMN_PART_SEP + "handle"),

  OWNER_ORGA_ID(TABLE_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN),
  OWNER_USER_ID(TABLE_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + UserProvider.ID_COLUMN),
  OWNER_REALM_ID(TABLE_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + "realm_id"),

  /**
   * Time Column
   */

  CREATION_TIME(TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX),
  MODIFICATION_TIME(TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX),


  USER_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "user" + COLUMN_PART_SEP + "count"),
  USER_IN_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "user" + COLUMN_PART_SEP + "in" + COLUMN_PART_SEP + "count"),
  LIST_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "list" + COLUMN_PART_SEP + "count"),
  APP_COUNT(TABLE_PREFIX + COLUMN_PART_SEP + "app" + COLUMN_PART_SEP + "count"),
  ;


  private final String columnName;

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
