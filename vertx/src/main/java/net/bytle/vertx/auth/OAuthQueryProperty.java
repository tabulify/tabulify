package net.bytle.vertx.auth;

/**
 * The query properties used in a OAuth request or response
 */
public enum OAuthQueryProperty {

  /**
   * Where to redirect when the auth is successful or not
   */
  REDIRECT_URI("redirect_uri"),
  /**
   * A nonce to avoid Cross request attack
   */
  STATE("state"),
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
   * Custom URL properties
   */

  /**
   * The realm identifier (guid or handle)
   */
  REALM_IDENTIFIER("realm_identifier"),

  /**
   * The list guid
   */
  LIST_GUID("list_guid");

  private final String key;

  OAuthQueryProperty(String redirectUri) {
    this.key = redirectUri;
  }

  @Override
  public String toString() {
    return key;
  }
}
