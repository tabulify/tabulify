package net.bytle.tower.util;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

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

}
