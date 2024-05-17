package net.bytle.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import net.bytle.exception.InternalException;

/**
 * A handler that does not fail when not logged in
 * as {@link io.vertx.ext.web.handler.APIKeyHandler} does
 */
public class ApiKeyHandlerRelaxed extends AuthenticationHandlerImpl<AuthenticationProvider> implements APIKeyHandler {

  @SuppressWarnings("FieldCanBeLocal")
  private String value = "X-API-KEY";

  public ApiKeyHandlerRelaxed(AuthenticationProvider authProvider) {
    super(authProvider);
  }

  @Override
  public ApiKeyHandlerRelaxed header(String headerName) {
    if (headerName == null) {
      throw new IllegalArgumentException("'headerName' cannot be null");
    }

    value = headerName;
    return this;
  }

  @Override
  public ApiKeyHandlerRelaxed parameter(String paramName) {
    throw new InternalException("URL parameter not supported");
  }

  @Override
  public ApiKeyHandlerRelaxed cookie(String cookieName) {
    throw new InternalException("Cookie Not supported");
  }

  @Override
  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {
    MultiMap headers = context.request().headers();
    if (headers != null && headers.contains(value)) {
      authProvider.authenticate(new TokenCredentials(headers.get(value)), authn -> {
        if (authn.failed()) {
          handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
        } else {
          handler.handle(authn);
        }
      });
      return;
    }
    /*
      Default: Return the actual user or null
     */
    User user = context.user();
    if (user != null) {
      handler.handle(Future.succeededFuture(user));
      return;
    }
    handler.handle(Future.succeededFuture());

  }
}
