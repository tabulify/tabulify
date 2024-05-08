package net.bytle.tower.eraldy.module.app.db;

import net.bytle.tower.eraldy.module.organization.db.OrganizationProvider;
import net.bytle.tower.eraldy.module.realm.db.UserProvider;
import net.bytle.vertx.db.JdbcColumn;
import net.bytle.vertx.db.JdbcSchemaManager;

import static net.bytle.tower.eraldy.module.app.db.AppProvider.APP_COLUMN_PREFIX;
import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public enum AppCols implements JdbcColumn {

  ID(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "id"),
  REALM_ID(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "realm_id"),
  NAME(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "name"),
  HANDLE(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "handle"),
  OWNER_ORGA_ID(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN),
  OWNER_USER_ID(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + UserProvider.ID_COLUMN),
  OWNER_REALM_ID(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + "realm_id"),
  HOME(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "home"),

  LIST_COUNT(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "list" + COLUMN_PART_SEP + "count"),


  CREATION_TIME(APP_COLUMN_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX),
  MODIFICATION_TIME(APP_COLUMN_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX),
  LOGO(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "logo"),
  TERM_OF_SERVICE(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "tos"),
  SLOGAN(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "slogan"),
  PRIMARY_COLOR(APP_COLUMN_PREFIX + COLUMN_PART_SEP + "primary_color");


  private final String columnName;

  AppCols(String s) {
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
