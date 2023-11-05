package net.bytle.vertx;



/**
 * Pojo that contains the OAuth authorization
 * that is stored temporarily until the
 * third party makes a request to get the access token
 * with the code attribute
 */
public class OAuthAuthorization {


  private String redirectUri;
  private UserClaims user;

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public UserClaims getUser() {
    return user;
  }

  public void setUser(UserClaims contextUser) {
    this.user = contextUser;
  }

}
