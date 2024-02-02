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
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.TowerFailureException;
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
  private final EraldyApiApp apiApp;

  public ApiKeyAndSessionUserAuthenticationHandler(EraldyApiApp eraldyApiApp, String headerName, ApiKeyAuthenticationProvider apiKeyUserProvider) {
    super(apiKeyUserProvider);
    this.apiKeyUserProvider = apiKeyUserProvider;
    this.headerName = headerName;
    this.apiApp = eraldyApiApp;
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
      /**
       * A call to the API should be logged.
       * If there is no session, it means that the clientId was not found
       */
      if (context.session() == null) {
        try {
          String clientId = apiApp.getAuthClientIdHandler().getClientId(context);
          // internal error
          handler.handle(Future.failedFuture(
            TowerFailureException.builder()
              .setMessage("The request has no session but a client id ("+clientId+")")
              .build())
          );
        } catch (NotFoundException e) {
          // no client id, the client has forgotten the client id in the http header
          handler.handle(Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
              .setMessage("The request has no client id.")
              .build())
          );
        }
        return;
      }
      handler.handle(Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .setMessage("The session has no authenticated user.")
          .build()
      ));
    }

  }
}
