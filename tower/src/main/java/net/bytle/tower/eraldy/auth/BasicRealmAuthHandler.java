package net.bytle.tower.eraldy.auth;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.BasicAuthHandlerImpl;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.tower.eraldy.model.openapi.Realm;

import java.nio.charset.StandardCharsets;

import static io.vertx.ext.auth.impl.Codec.base64Decode;

/**
 * Extension of {@link BasicAuthHandlerImpl}
 * that supports a realm
 */
@SuppressWarnings("unused")
public class BasicRealmAuthHandler extends BasicAuthHandlerImpl {


  public BasicRealmAuthHandler(AuthenticationProvider authProvider, EraldyDomain eraldyDomain) {
    super(authProvider, eraldyDomain.getRealmHandle());
  }

  public static AuthenticationHandler create(AuthenticationProvider authProvider, EraldyDomain eraldyDomain) {

    return new BasicRealmAuthHandler(authProvider, eraldyDomain);

  }

  @Override
  public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {

    /**
     * Parse the Authorization HTTP header
     */
    parseAuthorization(context, parseAuthorization -> {

      if (parseAuthorization.failed()) {
        handler.handle(Future.failedFuture(parseAuthorization.cause()));
        return;
      }

      /**
       * Basic Authentication
       */
      final String credentialName;
      final String credentialPassword;
      try {
        String decoded = new String(base64Decode(parseAuthorization.result()), StandardCharsets.UTF_8);
        int colonIdx = decoded.indexOf(":");
        if (colonIdx != -1) {
          credentialName = decoded.substring(0, colonIdx);
          credentialPassword = decoded.substring(colonIdx + 1);
        } else {
          credentialName = decoded;
          credentialPassword = null;
        }
      } catch (RuntimeException e) {
        handler.handle(Future.failedFuture(new HttpException(400, e)));
        return;
      }

      Realm realmObject = AuthRealmHandler.getFromRoutingContextKeyStore(context);
      ApiUsernamePasswordRealmCredentials credentials = new ApiUsernamePasswordRealmCredentials(credentialName, credentialPassword, realmObject);
      authProvider.authenticate(credentials, authn -> {
        if (authn.failed()) {
          handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
        } else {
          handler.handle(authn);
        }
      });

    });
  }
}
