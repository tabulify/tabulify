package net.bytle.tower.eraldy.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.impl.RoutingContextInternal;

/**
 * Source {@link io.vertx.ext.web.handler.impl.FormLoginHandlerImpl}
 * of {@link FormLoginHandler}
 * <p>
 * Example: <a href="https://github.com/lynndylanhurley/redux-auth/blob/master/docs/api-expectations/email-sign-in.md">...</a>
 * <p>
 * <a href="https://vertx.io/docs/#authentication-and-authorization">...</a>
 * <a href="https://vertx.io/docs/vertx-auth-common/java/">...</a>
 */
public class FormAuthenticationHandler extends AuthenticationHandlerImpl<AuthenticationProvider> {


  static final HttpException BAD_REQUEST = new HttpException(400);

  public FormAuthenticationHandler(AuthenticationProvider authProvider) {
    super(authProvider);
  }

  public static FormAuthenticationHandler create(AuthenticationProvider authenticationProvider) {
    return new FormAuthenticationHandler(authenticationProvider);
  }

  /**
   * This function is called by the {@link AuthenticationHandlerImpl#handle(RoutingContext)}
   */
  @Override
  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {

    HttpServerRequest req = context.request();

    if (req.method() != HttpMethod.POST) {
      /**
       * Must be a POST
       * {@link #postAuthentication(RoutingContext)} go to the mext
       */
      return;
    }

    if (!((RoutingContextInternal) context).seenHandler(RoutingContextInternal.BODY_HANDLER)) {
      handler.handle(Future.failedFuture(new NoStackTraceThrowable("BodyHandler is required to process POST requests")));
      return;
    }

    JsonObject body = context.body().asJsonObject();
    String login = body.getString("login");
    String password = body.getString(FormLoginHandler.DEFAULT_PASSWORD_PARAM);
    if (login == null || password == null) {
      handler.handle(Future.failedFuture(BAD_REQUEST));
      return;
    }

    User result = User.fromName(login);
    handler.handle(Future.succeededFuture(result));

  }

  @Override
  public void postAuthentication(RoutingContext ctx) {
    ctx.next();
  }

}
