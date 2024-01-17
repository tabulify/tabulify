package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonValue;

/**
* The status of the user on the list
*/
public enum ListUserStatus {

  OK(0),

  UNSUBSCRIBED(1);

  private final Integer value;

  ListUserStatus(Integer value) {
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

  public static ListUserStatus fromValue(Integer value) {
    for (ListUserStatus b : ListUserStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
