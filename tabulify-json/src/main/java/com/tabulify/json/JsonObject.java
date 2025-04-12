package com.tabulify.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class JsonObject {

  private final com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();

  /**
   * An instant needs a timezone to be formatted
   */
  static DateTimeFormatter formatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault());

  public static JsonObject create() {
    return new JsonObject();
  }


  public void print() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(this.jsonObject));
  }

  public JsonObject addProperty(String property, String value) {
    this.jsonObject.addProperty(property, value);
    return this;
  }

  public JsonObject addProperty(String property, Instant instant) {
    this.jsonObject.addProperty(property, formatter.format(instant));
    return this;
  }

  public JsonObject addProperty(String property, Path path) {
    this.jsonObject.addProperty(property, path.toString());
    return this;
  }

  public JsonObject addProperty(String property, Boolean b) {
    this.jsonObject.addProperty(property, b);
    return this;
  }

  public JsonObject addProperty(String property, Number number) {
    this.jsonObject.addProperty(property, number);
    return this;
  }

  public JsonObject createChildObject(String property) {
    JsonObject jsonObject = new JsonObject();
    this.jsonObject.add(property, jsonObject.jsonObject);
    return jsonObject;
  }

  public JsonObject addProperty(String property, Object value) {
    if (value.getClass().equals(String.class)) {
      this.addProperty(property, value.toString());
    } else if (value.getClass().equals(Instant.class)) {
      this.addProperty(property, (Instant) value);
    } else if (value.getClass().equals(Path.class)) {
      this.addProperty(property, (Path) value);
    } else if (value.getClass().equals(Boolean.class)) {
      this.addProperty(property, (Boolean) value);
    } else if (value instanceof Number) {
      this.addProperty(property, (Number) value);
    } else if (value instanceof Collection) {
      JsonArray jsonArray = new JsonArray();
      Collection<Object> col = (Collection<Object>) value;
      switch (col.size()) {
        case 0:
          break;
        case 1:
          this.addProperty(property, col.iterator().next());
          break;
        default:
          for (Object arrayElement : col) {
            if (arrayElement instanceof Boolean) {
              jsonArray.add((Boolean) arrayElement);
            } else if (arrayElement instanceof Number) {
              jsonArray.add((Number) arrayElement);
            } else if (arrayElement instanceof JsonObject) {
              jsonArray.add(((JsonObject) arrayElement).jsonObject);
            } else {
              jsonArray.add(arrayElement.toString());
            }
          }
          this.jsonObject.add(property, jsonArray);
      }
    } else {
      this.addProperty(property, value.toString());
    }
    return this;
  }
}
