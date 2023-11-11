package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.TowerApexDomain;
import net.bytle.vertx.auth.AuthQueryProperty;


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
      .build();

  }


  public static AuthRealmHandler createFrom(Router rootRouter, EraldyApiApp apiApp) {
    AuthRealmHandler authHandler = new AuthRealmHandler(apiApp);
    rootRouter.route().handler(authHandler);
    return authHandler;
  }


  private Future<Realm> getAuthRealm(RoutingContext routingContext) {


    HttpServerRequest request = routingContext.request();

    RealmProvider realmProvider = apiApp.getRealmProvider();


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
     * From the signed-in user
     */
    try {
      User user = this.apiApp.getAuthSignedInUser(routingContext);
      return realmProvider.getRealmFromIdentifier(user.getRealm().getGuid());
    } catch (NotFoundException e) {
      // not signed in
    }

    /**
     * From the cookie
     */
    try {
      return Future.succeededFuture(this.getAuthRealmFromCookie(routingContext));
    } catch (NullValueException e) {
      //
    }

    return Future.succeededFuture(EraldyRealm.get().getRealm());

  }

  private Realm getAuthRealmFromCookie(RoutingContext routingContext) throws NullValueException {
    Realm realm = authRealmCookie.getValue(routingContext);
    try {
      Guid realmGuid = this.apiApp.getRealmProvider().getGuidFromHash(realm.getGuid());
      realm.setLocalId(realmGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
    return realm;
  }

  /**
   *
   * @param realmIdentifier - the realm identifier
   * @param routingContext - the routing context
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
            context.fail(HttpStatus.BAD_REQUEST.httpStatusCode(), new IllegalArgumentException("The realm could not be determined."));
            return;
          }


          /**
           * We need to set the realm in a cookie for OAuth callback
           * Why?
           * Because the realm is mandatory as the state is stored in the session
           * and that the session is realm dependent
           */
          Realm frontEndRealm = apiApp.getRealmProvider().toEraldyFrontEnd(currentAuthRealm);
          authRealmCookie.setValue(frontEndRealm, context);


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
