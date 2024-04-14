package net.bytle.vertx.db;

import io.vertx.sqlclient.Row;

import java.time.LocalDateTime;

public class JdbcRow {
  private final Row row;

  public JdbcRow(Row row) {
    this.row = row;
  }

  public Long getLong(JdbcTableColumn column){
    return this.row.getLong(column.getColumnName());
  }

  public Integer getInteger(JdbcTableColumn column){
    return this.row.getInteger(column.getColumnName());
  }

  public String getString(JdbcTableColumn column){
    return this.row.getString(column.getColumnName());
  }

  public LocalDateTime getLocalDateTime(JdbcTableColumn column){
    return this.row.getLocalDateTime(column.getColumnName());
  }

}
