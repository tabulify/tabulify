package net.bytle.tower.eraldy.module.organization.db;

import net.bytle.vertx.db.JdbcColumn;

public enum OrganizationCols implements JdbcColumn {


  ID("orga_id"),
  HANDLE("orga_handle"),
  NAME("orga_name"),
  OWNER_ID("orga_owner_user_id"),
  REALM_ID("orga_owner_realm_id"),
  MODIFICATION_IME("orga_modification_time"),
  CREATION_TIME("orga_creation_time");

  private final String columnName;

  OrganizationCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }
}
