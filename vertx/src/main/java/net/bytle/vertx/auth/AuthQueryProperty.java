package net.bytle.vertx.auth;

/**
 * The query properties used in a auth request or response
 */
public enum AuthQueryProperty {

  /**
   * Where to redirect when the auth is successful or not
   */
  REDIRECT_URI("redirect_uri"),
  /**
   * A nonce to avoid Cross request attack
   */
  OAUTH_STATE("state"),
  /**
   * The code send in the callback when the auth is successful
   */
  CODE("code"),

  /**
   * The client id
   */
  CLIENT_ID("client_id"),

  /**
   * The type of authorization flow
   * (ie code, ...)
   */
  RESPONSE_TYPE("response_type"),


  /**
   * The list guid
   */
  LIST_GUID("list_guid"),

  /**
   * The proxied app guid
   */
  APP_GUID("app_guid");

  private final String key;

  AuthQueryProperty(String redirectUri) {
    this.key = redirectUri;
  }

  @Override
  public String toString() {
    return key;
  }
}
