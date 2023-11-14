package net.bytle.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public interface OAuthExternalProvider {

  void authenticate(Oauth2Credentials oAuthCodeCredentials, Handler<AsyncResult<User>> resultHandler);

  Future<JsonObject> userInfo(User oAuthUser);

  List<String> getRequestedScopes();

  Future<AuthUser> getEnrichedUser(RoutingContext ctx, JsonObject userInfo, String accessToken);

  /**
   * the public uri local where the callback should be called
   * by the provider
   */
  String getCallbackPublicUri();

  /**
   * The local path where to mount the callbacks
   */
  String getCallbackOperationPath();

  /**
   * @return a unique name for the provider
   */
  String getName();

  /**
   * The URL endpoint of the external provider that
   * we should call with a GET
   */

  String getAuthorizeUrl(RoutingContext context, String listGuid);

  /**
   *
   * @return a reference to the manager
   */
  OAuthExternal getOAuthExternal();

}
