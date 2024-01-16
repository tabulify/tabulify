package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How the user has been added to the list
 */
public enum ListUserFlow {

  OAUTH("oauth"),

  EMAIL("email"),

  IMPORT("import");

  private String value;

  ListUserFlow(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static ListUserFlow fromValue(String value) {
    for (ListUserFlow b : ListUserFlow.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
