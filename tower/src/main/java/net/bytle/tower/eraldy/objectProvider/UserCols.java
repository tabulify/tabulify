package net.bytle.tower.eraldy.objectProvider;

import net.bytle.vertx.db.JdbcTableColumn;

public enum UserCols implements JdbcTableColumn {


  ID("user_id"),
  REALM_ID("user_realm_id"),
  EMAIL_ADDRESS("user_email_address"),
  CREATION_IME("user_creation_time"),
  MODIFICATION_IME("user_modification_time"),
  DATA("user_data"),
  STATUS("user_status"),
  CREATION_TIME("user_creation_time"), PASSWORD("user_password");

  private final String columnName;

  UserCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }
}
