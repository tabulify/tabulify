package net.bytle.tower.util;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import net.bytle.vertx.db.JdbcColumn;
import net.bytle.vertx.db.JdbcRow;

public class Postgres {

  /**
   *
   */
  public static JsonObject getFromJsonB(Row row, String columnName) {
    /**
     * The Native Postgres driver return a PGObject
     * but with the Native Pg Client, we can return a JsonObject directly
     */
    Object value = row.getValue(columnName);
    if(value instanceof JsonObject){
      return (JsonObject) value;
    }
    if(value instanceof String){
      return new JsonObject((String) value);
    }
    return row.getJsonObject(columnName);
  }

  @SuppressWarnings("unused")
  public static JsonObject getFromJsonB(JdbcRow row, JdbcColumn columnName) {
    /**
     * The Native Postgres driver return a PGObject
     * but with the Native Pg Client, we can return a JsonObject directly
     */
    Object value = row.getValue(columnName);
    if(value instanceof JsonObject){
      return (JsonObject) value;
    }
    if(value instanceof String){
      return new JsonObject((String) value);
    }
    return row.getJsonObject(columnName);
  }

}
