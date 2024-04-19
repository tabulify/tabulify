package net.bytle.tower.eraldy.module.organization.db;

import net.bytle.vertx.db.JdbcTableColumn;

public enum OrganizationCols implements JdbcTableColumn {


  ID("orga_id"),
  HANDLE("orga_handle"),
  NAME("orga_name"),
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
