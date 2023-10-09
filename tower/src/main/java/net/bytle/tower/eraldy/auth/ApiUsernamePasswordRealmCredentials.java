package net.bytle.tower.eraldy.auth;

import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import net.bytle.tower.eraldy.model.openapi.Realm;

/**
 * A username credential that adds the realm
 *
 */
public class ApiUsernamePasswordRealmCredentials extends UsernamePasswordCredentials {


  /**
   * Note that for the superuser, supertoken, the realm may be null
   * (when we create a realm or an app)
   */
  private Realm realm;

  public ApiUsernamePasswordRealmCredentials(String username, String password, Realm realm) {
    setUsername(username);
    setPassword(password);
    setRealm(realm);
  }

  private ApiUsernamePasswordRealmCredentials setRealm(Realm realm) {
    this.realm = realm;
    return this;
  }


}
