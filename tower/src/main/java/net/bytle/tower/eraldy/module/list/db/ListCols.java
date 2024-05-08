package net.bytle.tower.eraldy.module.list.db;

import net.bytle.vertx.db.JdbcColumn;

public enum ListCols implements JdbcColumn {

  ID("list_id"),
  REALM_ID("list_realm_id"),
  HANDLE("list_handle"),
  NAME("list_name"),
  TITLE("list_title"),
  APP_ID("list_app_id"),
  OWNER_USER_ID("list_owner_user_id"),
  OWNER_REALM_ID("list_owner_realm_id"),
  OWNER_ORGA_ID("list_owner_orga_id"),
  USER_COUNT("list_user_count"),
  USER_IN_COUNT("list_user_in_count"),
  MAILING_COUNT("list_mailing_count"),
  CREATION_TIME("list_creation_time"),
  MODIFICATION_TIME("list_modification_time");


  private final String columnName;

  ListCols(String columnName) {
    this.columnName = columnName;
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
