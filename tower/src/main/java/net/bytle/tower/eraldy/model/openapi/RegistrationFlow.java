package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How the user has been registered
 */
public enum RegistrationFlow {

  OAUTH("oauth"),

  EMAIL("email"),

  IMPORT("import");

  private final String value;

  RegistrationFlow(String value) {
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

  public static RegistrationFlow fromValue(String value) {
    for (RegistrationFlow b : RegistrationFlow.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
