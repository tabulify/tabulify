package net.bytle.tower.eraldy.module.organization.db;

import net.bytle.vertx.db.JdbcColumn;

public enum OrganizationRoleCols implements JdbcColumn {


  ID("orga_role_id"),
  NAME("orga_role_name"),
  MODIFICATION_TIME("orga_role_modification_time"),
  CREATION_TIME("orga_role_creation_time");

  private final String columnName;

  OrganizationRoleCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }
}
