package net.bytle.tower.eraldy.auth;

public enum AuthUserScope {

  ANALYTICS_EVENT_GET("get an analytics event", false),
  REALM_GET("get a realm", false),
  REALM_APPS_GET("list the apps of a realm", false),
  REALM_OWNER_GET("get the realm owner", false),
  REALM_USER_GET("get a realm user", false),
  REALM_USER_UPDATE("update a realm user", false),
  LIST_CREATE("create a list", false),
  /**
   * To be able to subscribe public user to a list, the list data needs to be public
   */
  LIST_GET("get a list", true),
  /**
   * To be able to subscribe public user to a list, the list owner data needs to be public
   */
  LIST_OWNER_GET("get the list owner", true),
  LIST_APP_GET("get the app of a list", false),
  LIST_DELETE("delete a list", false),
  LIST_USERS_GET("get the users of a list", false),
  LIST_UPDATE("update a list", false),
  ORGA_GET("get an organization", false),
  ORGA_USER_GET("get an organizational user", false),
  ORGA_USERS_GET("get organisational users", false),
  APP_CREATE("create an app", false),
  APP_UPDATE("update an app", false),
  APP_GET("get an app", false),
  APP_OWNER_GET("get the app owner", false),
  APP_LISTS_GET("get the lists of an app", false),
  LIST_IMPORT("import a list", false),

  REALM_LISTS_GET("get the lists of a realm", false),
  MAILING_GET("get a mailing", false),
  LIST_MAILINGS_GET("get the mailings of a list", false),
  MAILING_UPDATE("update of mailing", false),
  MAILING_CREATE("create a mailing", false),
  MAILING_SEND_TEST_EMAIL("send a test email", false),
  MAILING_EXECUTE("execute a mailing", false),
  MAILING_JOBS_GET("get one or more mailing jobs", false),
  MAILING_ITEM_DELIVER("deliver a mailing item (send email)", false),
  MAILING_AUTHOR_GET("get the author of a mailing", false),
  MAILING_LIST_GET("get the list of a mailing", false),
  MAILING_ITEMS_GET("get an mailing item", false);


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
