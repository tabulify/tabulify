package net.bytle.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;

public abstract class OAuthExternalProviderAbs implements OAuthExternalProvider {

  private final OAuth2Auth authProvider;

  public OAuthExternalProviderAbs(OAuth2Auth authProvider) {
    this.authProvider = authProvider;
  }

  @Override
  public String authorizeURL(OAuth2AuthorizationURL authorizationURL) {

    authorizationURL
      .setScopes(getRequestedScopes());
    return this.authProvider.authorizeURL(authorizationURL);

  }

  @Override
  public void authenticate(Oauth2Credentials oAuthCodeCredentials, Handler<AsyncResult<User>> resultHandler) {
    this.authProvider.authenticate(oAuthCodeCredentials, resultHandler);
  }

  @Override
  public Future<JsonObject> userInfo(User oAuthUser) {
    return this.authProvider.userInfo(oAuthUser);
  }

}
