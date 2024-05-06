package net.bytle.tower.eraldy.auth;

public enum AuthUserScope {

  ANALYTICS_EVENT_GET("get an analytics event", false),
  REALM_APPS_GET("list the apps of a realm", false),
  REALM_USER_GET("Get a user", false),
  REALM_USER_UPDATE("update a realm user", false),
  LIST_CREATION("create a list", false),
  /**
   * To be able to subscribe public user to a list, the list data needs to be public
   * It opens the door to get all data we want.
   */
  LIST_GET("get a list", true),
  LIST_DELETE("delete a list", false),
  LIST_GET_USERS("get users from a list", false),
  LIST_PATCH("patch a list", false),
  ORGA_GET("get an organization", false),
  ORGA_USER_GET("get an organizational user", false),
  ORGA_USERS_GET("get organisational users", false),
  APP_CREATE("create an app", false),
  APP_UPDATE("update an app", false),
  APP_GET("get an app", false),
  APP_LISTS_GET("get the lists of an app", false),
  LIST_IMPORT("import a list", false),

  REALM_LISTS_GET("get the lists of a realm", false),
  MAILING_GET("get a mailing", false),
  MAILINGS_LIST_GET("get the mailings for a list", false),
  MAILING_UPDATE("update of mailing", false),
  MAILING_CREATE("create a mailing", false),
  MAILING_SEND_TEST_EMAIL("send a test email", false),
  MAILING_EXECUTE("execute a mailing", false),
  MAILING_JOBS_GET("get mailing jobs", false),
  MAILING_DELIVER_ITEM("deliver a mailing item (send email)", false),
  ;


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

  /**
   * Public action for all clients
   */
  public boolean isPublic() {
    return this.isPublic;
  }

  @SuppressWarnings("unused")
  public String getHumanActionName() {
    return humanActionName;
  }
}
