package net.bytle.tower.eraldy.module.list.db;

import net.bytle.vertx.db.JdbcColumn;

public enum ListUserCols implements JdbcColumn {

  USER_ID("list_user_user_id"),
  REALM_ID("list_user_realm_id"),
  LIST_ID("list_user_list_id"),
  STATUS_CODE("list_user_status_code"),
  STATUS_MESSAGE("list_user_status_message"),
  IN_SOURCE_ID("list_user_in_source_id"),
  IN_OPT_IN_ORIGIN("list_user_in_opt_in_origin"),
  IN_OPT_IN_IP("list_user_in_opt_in_ip"),
  IN_OPT_IN_TIME("list_user_in_opt_in_time"),
  IN_OPT_IN_CONFIRMATION_IP("list_user_in_opt_in_confirmation_ip"),
  IN_OPT_IN_CONFIRMATION_TIME("list_user_in_opt_in_confirmation_time"),
  OUT_OPT_OUT_TIME("list_user_out_opt_out_time"),

  CREATION_TIME("list_user_creation_time"),
  MODIFICATION_TIME("list_user_modification_time");


  private final String columnName;

  ListUserCols(String columnName) {
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
