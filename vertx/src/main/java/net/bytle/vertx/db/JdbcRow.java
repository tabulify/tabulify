package net.bytle.vertx.db;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

import java.time.LocalDateTime;

public class JdbcRow {
  private final Row row;

  public JdbcRow(Row row) {
    this.row = row;
  }

  public Long getLong(JdbcColumn column) {
    return this.row.getLong(column.getColumnName());
  }

  public Integer getInteger(JdbcColumn column, Integer defaultValue) {
    Integer intNumber = this.row.getInteger(column.getColumnName());
    if (intNumber == null) {
      return defaultValue;
    }
    return intNumber;
  }

  public Integer getInteger(JdbcColumn column) {
    return getInteger(column, null);
  }


  public String getString(JdbcColumn column) {
    return this.row.getString(column.getColumnName());
  }

  public LocalDateTime getLocalDateTime(JdbcColumn column) {
    return this.row.getLocalDateTime(column.getColumnName());
  }

  public Object getValue(JdbcColumn column) {
    return this.row.getValue(column.getColumnName());
  }

  public JsonObject getJsonObject(JdbcColumn column) {
    return this.row.getJsonObject(column.getColumnName());
  }

}
