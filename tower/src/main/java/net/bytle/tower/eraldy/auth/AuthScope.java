package net.bytle.tower.eraldy.auth;

public enum AuthScope {

  ANALYTICS_EVENT_GET("get an analytics event"),
  REALM_APPS_GET("get apps"),
  REALM_APP_GET("get an app"),
  REALM_USER_GET("Get a user"),
  LIST_CREATION("create a list"),
  LIST_GET("get a list"),
  LIST_DELETE("delete a list"),
  LIST_GET_USERS("get users from a list"),
  LIST_PATCH("patch a list"),
  ORGA_USERS_GET("get organisational users"),
  APP_CREATE("create an app"),
  LIST_IMPORT("import a list"),
  LIST_ADD_USER_FLOW("add a user to a list via a flow"),
  LOGIN_EMAIL("send a email login"),
  PASSWORD_RESET_FLOW("reset a password via a flow"),
  USER_REGISTRATION_FLOW("register a user via a flow"),
  PROXY_CLIENT("proxy a client");

  private final String humanActionName;

  /**
   *
   * @param humanActionName - the text that should come after `you don't have the permission to`
   */
  AuthScope(String humanActionName) {
    this.humanActionName = humanActionName;
  }

  public String getHumanActionName() {
    return humanActionName + " (" + name().toLowerCase() + ").";
  }

}
