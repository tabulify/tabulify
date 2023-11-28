package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApexDomain;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthQueryProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The cookie session is at the realm level.
 * <p>
 * This handler determines the authentication realm of the request.
 * If none can be determined, the realm is the Eraldy realm.
 * <p>
 * After the handler has been executed,
 * next handlers can retrieve the realm with the function {@link #getFromRoutingContextKeyStore(RoutingContext)}
 */
public class AuthRealmHandler implements Handler<RoutingContext> {

  static Logger LOGGER = LogManager.getLogger(AuthRealmHandler.class);
  /**
   * The realm key in the vertx routing context
   */
  private static final String AUTH_REALM_CONTEXT_KEY = "auth-realm-context-key";
  private static final String X_AUTH_REALM_IDENTIFIER = "X-AUTH-REALM-IDENTIFIER";

  private final EraldyApiApp apiApp;
  private final FrontEndCookie<Realm> authRealmCookie;

  private AuthRealmHandler(EraldyApiApp apiApp) {

    this.apiApp = apiApp;
    /**
     * The cookie that stores the realm information (when the front end is loaded)
     */
    String cookieName = this.apiApp.getApexDomain().getPrefixName() + "-auth-realm";
    this.authRealmCookie = FrontEndCookie.conf(cookieName, Realm.class)
      .setPath("/") // send back from all pages
      .setJsonMapper(this.apiApp.getRealmProvider().getPublicJsonMapper())
      .build();

  }


  public static AuthRealmHandler createFrom(Router rootRouter, EraldyApiApp apiApp) {
    AuthRealmHandler authHandler = new AuthRealmHandler(apiApp);
    rootRouter.route().handler(authHandler);
    return authHandler;
  }


  private Future<Realm> getAuthRealm(RoutingContext routingContext) {


    HttpServerRequest request = routingContext.request();

    /**
     * From Query String
     */
    String realmIdentifier = request.getParam(AuthQueryProperty.REALM_IDENTIFIER.toString());
    if (realmIdentifier != null) {
      return this.getAuthRealmFromDatabaseOrCookie(realmIdentifier, routingContext);
    }

    /**
     * From header
     */
    realmIdentifier = request.getHeader(X_AUTH_REALM_IDENTIFIER);
    if (realmIdentifier != null) {
      return getAuthRealmFromDatabaseOrCookie(realmIdentifier, routingContext);
    }

    /**
     * Note, from the signed-in user, it will not work
     * because the session is not known as it depends on the realm
     * , and therefore we don't know the logged-in user yet
     */

    /**
     * From the cookie
     */
    try {
      return Future.succeededFuture(this.getAuthRealmFromCookie(routingContext));
    } catch (NullValueException e) {
      //
    }

    /**
     * Eraldy realm has default
     */
    return Future.succeededFuture(EraldyRealm.get().getRealm());


  }

  private Realm getAuthRealmFromCookie(RoutingContext routingContext) throws NullValueException {
    Realm realm;
    try {
      realm = authRealmCookie.getValue(routingContext);
    } catch (CastException e) {
      LOGGER.error("Error while reading the auth realm cookie", e);
      throw new NullValueException();
    }
    try {
      Guid realmGuid = this.apiApp.getRealmProvider().getGuidFromHash(realm.getGuid());
      realm.setLocalId(realmGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      LOGGER.error("Error while reading the auth realm guid", e);
      throw new NullValueException();
    }
    return realm;
  }

  /**
   * @param realmIdentifier - the realm identifier
   * @param routingContext  - the routing context
   * @return the realm from the cookie if the identifier match otherwise from the database
   */
  private Future<Realm> getAuthRealmFromDatabaseOrCookie(String realmIdentifier, RoutingContext routingContext) {
    try {
      Realm realm = this.getAuthRealmFromCookie(routingContext);
      if (this.apiApp.getRealmProvider().isIdentifierForRealm(realmIdentifier, realm)) {
        return Future.succeededFuture(realm);
      }
    } catch (NullValueException e) {
      // no realm cookie
    }
    return this.apiApp.getRealmProvider().getRealmFromIdentifier(realmIdentifier);
  }


  @Override
  public void handle(RoutingContext context) {


    getAuthRealm(context)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, context))
      .onSuccess(currentAuthRealm -> {
          if (currentAuthRealm == null) {

            /**
             * The realm is mandatory
             * Even if we get an app or list guid that contains the realm,
             * it's too difficult to manage the {@link EraldySessionHandler session}
             * at the api implementation (too late in the calls)
             */
            context.fail(TowerFailureTypeEnum.BAD_REQUEST_400.getStatusCode(), new IllegalArgumentException("The realm could not be determined."));
            return;
          }


          /**
           * We need to set the realm in a cookie
           * * for the frontend. They read and get the realm this way.
           * * for OAuth callback. Why?
           * Because the realm is mandatory as the state is stored in the session
           * and that the session is realm dependent.
           */
          authRealmCookie.setValue(currentAuthRealm, context);


          /**
           * To retrieve the request realm quickly
           * via {@link RealmProvider#getAuthRealm(RoutingContext, TowerApexDomain)}
           */
          context.put(AUTH_REALM_CONTEXT_KEY, currentAuthRealm);
          context.next();

        }
      );

  }

  /**
   * @param routingContext - the routing context
   * @return the realm or throw
   */
  public static Realm getFromRoutingContextKeyStore(RoutingContext routingContext) {
    Realm realm = routingContext.get(AuthRealmHandler.AUTH_REALM_CONTEXT_KEY);
    if (realm == null) {
      throw new InternalException("The authentication realm was not found, does the auth realm handler was executed before in the route hierarchy");
    }
    return realm;
  }


}
