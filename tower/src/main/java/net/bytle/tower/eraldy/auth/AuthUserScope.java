package net.bytle.tower.eraldy.auth;

public enum AuthUserScope {

  ANALYTICS_EVENT_GET("get an analytics event", false),
  REALM_APPS_GET("get apps", false),
  REALM_APP_GET("get an app", false),
  REALM_USER_GET("Get a user", false),
  LIST_CREATION("create a list", false),
  /**
   * To be able to subscribe public user to a list, the list data needs to be public
   */
  LIST_GET("get a list", true),
  LIST_DELETE("delete a list", false),
  LIST_GET_USERS("get users from a list", false),
  LIST_PATCH("patch a list", false),
  ORGA_USERS_GET("get organisational users", false),
  APP_CREATE("create an app", false),
  LIST_IMPORT("import a list", false),

  APP_LISTS_GET("get the lists of an app", false),
  REALM_LISTS_GET("get the lists of a realm", false);


  private final String humanActionName;
  private final boolean isPublic;

  /**
   * @param humanActionName - the text that should come after `you don't have the permission to`
   * @param isPublic - if the action can be accessed by an anonymous user
   */
  AuthUserScope(String humanActionName, boolean isPublic) {

    this.humanActionName = humanActionName;
    this.isPublic = isPublic;

  }

  public String getHumanActionName() {
    return humanActionName + " (" + name().toLowerCase() + ").";
  }

  /**
   * Public action for all clients
   */
  public boolean isPublic() {
    return this.isPublic;
  }

}
