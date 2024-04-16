package net.bytle.tower.eraldy.objectProvider;

import net.bytle.vertx.db.JdbcTableColumn;

public enum UserCols implements JdbcTableColumn {


  ID("realm_user_id"),
  REALM_ID("realm_user_realm_id"),
  EMAIL_ADDRESS("realm_user_email_address");

  private String columnName;

  UserCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }
}
