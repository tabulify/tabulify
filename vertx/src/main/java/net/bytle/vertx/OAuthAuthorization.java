package net.bytle.vertx;


import net.bytle.vertx.auth.AuthUser;

/**
 * Pojo that contains the OAuth authorization
 * that is stored temporarily until the
 * third party makes a request to get the access token
 * with the code attribute
 */
public class OAuthAuthorization {

  private String redirectUri;
  private AuthUser user;

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public AuthUser getUser() {
    return user;
  }

  public void setAuthUser(AuthUser contextUser) {
    this.user = contextUser;
  }

}
