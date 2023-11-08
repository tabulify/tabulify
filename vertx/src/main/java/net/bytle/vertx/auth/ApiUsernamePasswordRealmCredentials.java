package net.bytle.vertx.auth;

import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

/**
 * A username credential that adds the realm
 *
 */
public class ApiUsernamePasswordRealmCredentials extends UsernamePasswordCredentials {


  /**
   * Note that for the superuser, supertoken, the realm may be null
   * (when we create a realm or an app)
   */
  private String realm;

  public ApiUsernamePasswordRealmCredentials(String username, String password, String realm) {
    setUsername(username);
    setPassword(password);
    setRealm(realm);
  }

  private ApiUsernamePasswordRealmCredentials setRealm(String realm) {
    this.realm = realm;
    return this;
  }


}
