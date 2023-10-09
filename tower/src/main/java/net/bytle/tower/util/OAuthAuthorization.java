package net.bytle.tower.util;


import net.bytle.tower.eraldy.model.openapi.User;

/**
 * Pojo that contains the OAuth authorization
 * that is stored temporarily until the
 * third party makes a request to get the access token
 * with the code attribute
 */
public class OAuthAuthorization {


  private String redirectUri;
  private User user;

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User contextUser) {
    this.user = contextUser;
  }

}
