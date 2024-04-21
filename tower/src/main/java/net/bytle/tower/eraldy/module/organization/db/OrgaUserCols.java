package net.bytle.tower.eraldy.module.organization.db;

import net.bytle.vertx.db.JdbcColumn;

public enum OrgaUserCols implements JdbcColumn {


  USER_ID("orga_user_user_id"),
  ORGA_ID("orga_user_orga_id"),
  ROLE_ID("orga_user_role_id"),
  MODIFICATION_IME("orga_user_modification_time"),
  CREATION_TIME("orga_user_creation_time");

  private final String columnName;

  OrgaUserCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }
}
