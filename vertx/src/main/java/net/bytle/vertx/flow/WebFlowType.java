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

  private final Integer id;

  WebFlowType(Integer id) {
    this.id = id;
  }

  @JsonValue
  public Integer getId() {
    return id;
  }

  @JsonValue
  public String getHandle() {
    return toString();
  }


  public static WebFlowType fromValue(Integer value) {
    for (WebFlowType b : WebFlowType.values()) {
      if (b.id.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
