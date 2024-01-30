package net.bytle.tower.eraldy.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import net.bytle.vertx.TowerFailureTypeEnum;

/**
 * A class that permits to
 * handle the authentication in OpenApi
 * (apikey + cookie session)
 */
public class ApiAuthenticationHandler extends AuthenticationHandlerImpl<AuthenticationProvider> {




  public ApiAuthenticationHandler() {
    super(null);
  }

  @Override
  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {
    User user = context.user();
    if (user != null) {
      handler.handle(Future.succeededFuture(user));
    } else {
      handler.handle(Future.failedFuture(new HttpException(TowerFailureTypeEnum.NOT_LOGGED_IN_401.getStatusCode(), "The session has no authenticated user.")));
    }
  }
}
