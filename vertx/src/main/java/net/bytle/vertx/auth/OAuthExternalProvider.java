package net.bytle.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public interface OAuthExternalProvider {

  void authenticate(Oauth2Credentials oAuthCodeCredentials, Handler<AsyncResult<User>> resultHandler);

  Future<JsonObject> userInfo(User oAuthUser);

  List<String> getRequestedScopes();

  Future<AuthUser> getEnrichedUser(RoutingContext ctx, JsonObject userInfo, String accessToken);

  void addCallBackHandler(Router router);

  /**
   * @return a unique name
   */
  String getName();

  String getAuthorizeUrl(RoutingContext context, String listGuid);

}
