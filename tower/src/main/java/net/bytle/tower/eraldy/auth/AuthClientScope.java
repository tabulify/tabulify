package net.bytle.tower.eraldy.auth;

/**
 * Scope for client
 * A client is special as it doesn't need to be
 * logged in
 */
public enum AuthClientScope {

  PROXY_CLIENT("proxy a client", false),
  PROXY_APP("proxy an app", false),
  LIST_ADD_USER_FLOW("add a user to a list via a flow", true),
  LOGIN_EMAIL_FLOW("send a email login", false),
  PASSWORD_RESET_FLOW("reset a password via a flow", false),
  USER_REGISTRATION_FLOW("register a user via a flow", false);

  private final String humanActionName;
  private final boolean isPublic;

  /**
   * @param humanActionName - the text that should come after `you don't have the permission to`
   * @param isPublic - if the action can be accessed by an anonymous user
   */
  AuthClientScope(String humanActionName, boolean isPublic) {

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
