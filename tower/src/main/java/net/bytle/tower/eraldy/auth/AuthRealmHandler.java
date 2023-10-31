package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.app.memberapp.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.util.OAuthQueryProperty;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;

import java.net.URI;


/**
 * Determine the authentication realm of the request
 * After the handler has been executed,
 * code can retrieve the realm with the function {@link #getFromRoutingContextKeyStore(RoutingContext)}
 */
public class AuthRealmHandler implements Handler<RoutingContext> {

  /**
   * The realm key in the context
   */
  private static final String AUTH_REALM_CONTEXT_KEY = "auth-realm-context-key";
  private static final String X_AUTH_REALM_HEADER_HANDLE = "X-AUTH-REALM-HANDLE";
  private static final String X_AUTH_REALM_HEADER_GUID = "X-AUTH-REALM-GUID";

  private final EraldyDomain eraldyDomain;

  private AuthRealmHandler(EraldyDomain eraldyDomain) {
    this.eraldyDomain = eraldyDomain;
  }


  public static AuthRealmHandler createFrom(Router rootRouter, EraldyDomain eraldyDomain) {
    AuthRealmHandler authHandler = new AuthRealmHandler(eraldyDomain);
    String routePath = eraldyDomain.getAbsoluteLocalPath() + "/*";
    rootRouter.route(routePath).handler(authHandler);
    return authHandler;
  }


  private Future<Realm> getAuthRealm(RoutingContext routingContext) {

    /**
     * Eraldy domain except Member App ? If yes, we know the realm
     * without accessing the database
     */
    HttpServerRequest request = routingContext.request();
    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    UriEnhanced requestUri = routingContextWrapper.getOriginalRequestAsUri();

    RealmProvider realmProvider = RealmProvider.createFrom(routingContext.vertx());

    /**
     * IF this is an eraldy app except the `member` app (it accepts other realm as parameter),
     * return the Eraldy realm
     */
    if (!requestUri.getHostWithPort().equals(EraldyMemberApp.get().getPublicDomainHost())) {
      String apexNameWithoutPort = eraldyDomain.getApexNameWithoutPort();
      String requestHost = requestUri.getApexWithoutPort();
      if (requestHost.equals(apexNameWithoutPort)) {
        return realmProvider.getRealmFromHandle(eraldyDomain.getRealmHandle());
      }
    }


    /**
     * From header
     */
    String realmHandle = request.getHeader(X_AUTH_REALM_HEADER_HANDLE);
    if (realmHandle != null) {
      return realmProvider.getRealmFromHandle(realmHandle);
    }
    String realmGuid = request.getHeader(X_AUTH_REALM_HEADER_GUID);
    if (realmGuid != null) {
      return realmProvider.getRealmFromGuid(realmGuid);
    }


    /**
     * Member App?
     */
    if (EraldyMemberApp.get().isAppRequest(routingContext)) {

      /**
       * In query properties?
       */
      realmHandle = request.getParam(OAuthQueryProperty.REALM_HANDLE.toString());
      if (realmHandle != null) {
        return realmProvider.getRealmFromHandle(realmHandle);
      }
      realmGuid = request.getParam(OAuthQueryProperty.REALM_GUID.toString());
      if (realmGuid != null) {
        return realmProvider.getRealmFromGuid(realmGuid);
      }

      /**
       * From the redirect URI for the Eraldy domain
       * (ie if the redirect URI is an eraldy domain without any realm query parameters, this is the eraldy realm)
       * We do it after the query parameters and header because they have
       * a higher priority
       */
      String redirectUri = request.getParam(OAuthQueryProperty.REDIRECT_URI.toString());
      UriEnhanced redirectUriAsUri;
      try {
        redirectUriAsUri = UriEnhanced.createFromString(redirectUri);
        String eraldyApexNameWithoutPort = eraldyDomain.getApexNameWithoutPort();
        String redirectUriHost = redirectUriAsUri.getApexWithoutPort();
        if (redirectUriHost.equals(eraldyApexNameWithoutPort)) {
          return realmProvider.getRealmFromHandle(eraldyDomain.getRealmHandle());
        }
      } catch (IllegalStructure e) {
        // should not happen
      }

      /**
       * From the Auth Realm Cookie
       * Use for {@link net.bytle.tower.util.OAuthExternal.AuthOAuthCallbackHandler}
       */
      try {
        Realm cookieRealm = getAuthRealmCookie(routingContext).getValue();
        return realmProvider.getRealmFromHandle(cookieRealm.getHandle());
      } catch (NullValueException e) {
        // not found
      }


    }

    /**
     * Determine with the database
     */
    return this.getAuthRealmFromHttpHost(routingContext);
  }


  /**
   * @param routingContext - the request context
   * @return the realm if found
   */
  private Future<Realm> getAuthRealmFromHttpHost(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();

    String authenticationScope = request.authority().host();
    /**
     * The scheme is mandatory
     * Otherwise the host is seen as path
     */
    URI uri = URI.create(request.scheme() + "://" + authenticationScope);
    try {
      AppProvider.validateDomainUri(uri);
    } catch (IllegalStructure e) {
      throw new InternalException("The URI (" + uri + ") is not valid." + e.getMessage(), e);
    }

    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
    String sql = "SELECT realm.*, app_uri FROM cs_realms.realm INNER JOIN cs_realms.realm_app ON cs_realms.realm.realm_id = cs_realms.realm_app.app_realm_id and app_uri = $1 order by app_uri limit 1";
    String likeScopeValue = uri.toString();
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(likeScopeValue))
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realmRows -> {

        if (realmRows.size() == 0) {
          return Future.succeededFuture(null);
        }

        if (realmRows.size() != 1) {
          return Future.failedFuture(new InternalException("The scope (" + authenticationScope + ") returns  more than one realm (" + realmRows.size() + ")"));
        }

        Row row = realmRows.iterator().next();

        return RealmProvider.createFrom(routingContext.vertx()).getRealmFromDatabaseRow(row, Realm.class);

      });
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
            context.fail(HttpStatus.BAD_REQUEST, new IllegalArgumentException("The realm could not be determined."));
            return;
          }

          EraldyMemberApp eraldyMemberApp = EraldyMemberApp.get();
          if (eraldyMemberApp.isAppRequest(context)) {

            /**
             * We need to set the realm in a cookie for OAuth callback
             * Why?
             * Because the realm is mandatory as the state is stored in the session
             * and that the session is realm dependent
             */
            FrontEndCookie<Realm> authRealmCookie = getAuthRealmCookie(context);
            Realm frontEndRealm = RealmProvider.createFrom(context.vertx()).toEraldyFrontEnd(currentAuthRealm);
            authRealmCookie.setValue(frontEndRealm);

          }

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
      throw new InternalException("The realm was not found, does the auth realm handler was executed before in the route hierarchy");
    }
    return realm;
  }

  private FrontEndCookie<Realm> getAuthRealmCookie(RoutingContext routingContext) {

    String cookieName = EraldyMemberApp.get().getApexDomain().getPrefixName() + "-auth-realm";
    return FrontEndCookie.conf(routingContext, cookieName, Realm.class)
      .setPath("/") // available to all pages
      .build();

  }

}
