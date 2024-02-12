package net.bytle.vertx.flow;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A flow is a process.
 * This information is used to track the origin of event (insertion, login, ...)
 * <p>
 * Flow was chosen as name because the app is a web app
 * and the process in the web are known as web flow (ie the oauth web flow)
 */
public enum FlowType {

  /**
   * At the server startup, the initial set up of the Eraldy model.
   * When the app starts for the first time, the initial objects
   * (realm, user, organization, ...) needs to be inserted
   */
  SERVER_STARTUP(0),
  /**
   * User sign-in and sign-up via Open Auth.
   */
  OAUTH(1),
  /**
   * User registration by email
   */
  USER_REGISTRATION(2),
  /**
   * List registration by email
   */
  LIST_REGISTRATION(3),
  /**
   * Login via Email
   */
  EMAIL_LOGIN(4),
  /**
   * List User Import
   */
  LIST_IMPORT(5),
  /**
   * Reset of password via mail
   */
  PASSWORD_RESET(6),
  /**
   * Login via Password
   */
  PASSWORD_LOGIN(7);


  private final Integer id;

  FlowType(Integer id) {
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


  @SuppressWarnings("unused")
  public static FlowType fromValue(Integer value) {
    for (FlowType b : FlowType.values()) {
      if (b.id.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
