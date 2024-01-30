package net.bytle.tower.eraldy.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;

/**
 * A class that permits to
 * handle the authentication in OpenApi
 * (apikey + cookie session)
 */
public class ApiKeyAndSessionUserAuthenticationHandler extends AuthenticationHandlerImpl<AuthenticationProvider> {


  private final String headerName;
  private final ApiKeyAuthenticationProvider apiKeyUserProvider;

  public ApiKeyAndSessionUserAuthenticationHandler(String headerName, ApiKeyAuthenticationProvider apiKeyUserProvider) {
    super(apiKeyUserProvider);
    this.apiKeyUserProvider = apiKeyUserProvider;
    this.headerName = headerName;
  }

  @Override
  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {

    /**
     * Do we have a Header
     */
    MultiMap headers = context.request().headers();
    if (headers != null && headers.contains(headerName)) {
      /**
       * Pass the authentication credentials to the provider
       * and handle the results
       */
      String token = headers.get(headerName);
      TokenCredentials credentials = new TokenCredentials(token);
      apiKeyUserProvider.authenticate(credentials, asyncUser -> {
        if (asyncUser.failed()) {
          handler.handle(Future.failedFuture(new HttpException(401, asyncUser.cause())));
        } else {
          handler.handle(asyncUser);
        }
      });
      return;
    }

    User user = context.user();
    if (user != null) {
      handler.handle(Future.succeededFuture(user));
    } else {
      handler.handle(Future.failedFuture(new HttpException(TowerFailureTypeEnum.NOT_LOGGED_IN_401.getStatusCode(), "The session has no authenticated user.")));
    }

  }
}
