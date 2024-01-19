package net.bytle.vertx.flow;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WebFlowType {

  /**
   * User sign-in and sign-up via external OAuth
   */
  EXTERNAL_OAUTH(0),
  /**
   * User registration by email
   */
  USER_REGISTRATION(1),
  /**
   * List registration by email
   */
  LIST_REGISTRATION(2),
  /**
   * Login via Email
   */
  EMAIL_LOGIN(3),
  /**
   * List User Import
   */
  LIST_IMPORT(4),
  /**
   * Reset of password via mail
   */
  PASSWORD_RESET(5),
  /**
   * Login via Password
   */
  PASSWORD_LOGIN(6);

  private final Integer value;

  WebFlowType(Integer value) {
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

  public static WebFlowType fromValue(Integer value) {
    for (WebFlowType b : WebFlowType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
