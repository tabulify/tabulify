package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonValue;

/**
* How the user has been added to the list
*/
public enum ListUserFlow {

  EMAIL(0),

  OAUTH(1),

  IMPORT(2);

  private final Integer value;

  ListUserFlow(Integer value) {
    this.value = value;
  }

  @JsonValue
  public Integer getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static ListUserFlow fromValue(Integer value) {
    for (ListUserFlow b : ListUserFlow.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
