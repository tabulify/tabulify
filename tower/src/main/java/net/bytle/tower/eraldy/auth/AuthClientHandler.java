package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.HttpServer;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

import java.util.Set;

import static net.bytle.vertx.auth.ApiKeyAuthenticationProvider.API_KEY_PROVIDER_ID;
import static net.bytle.vertx.auth.ApiKeyAuthenticationProvider.ROOT_AUTHORIZATION;


/**
 * The Client
 * This handler determines the client (therefore also the authentication realm) of the request.
 * <p>
 * The auth realm is derived from the client.
 * The request are made by client that are attached to an app
 * Because an app is attached to a realm, we can derive it.
 * The client is then identified and the realm derived from it.
 * <p>
 * This mechanism permits to delete entirely the realm from the API
 * and adds a client identification.
 * <p>
 * Note: Because the cookie session is at the realm level, this handler adds also the realm name on the context
 * for the session.
 * <p>
 * After the handler has been executed,
 * next handlers can retrieve the realm with the function {@link #getApiClientStoredOnContext(RoutingContext)}
 */
public class AuthClientHandler implements Handler<RoutingContext> {

  private static final String CLIENT_ID_CONTEXT_KEY = "client-id-context-key";

  private static final String X_CLIENT_ID = "X-CLIENT-ID";

  /**
   * The client id
   */
  private static final String CLIENT_ID = "client_id";

  private final EraldyApiApp apiApp;

  /**
   * The cookie that stores the last realm
   * information (when the front end is loaded)
   */
  private final FrontEndCookie<Realm> lastAuthRealmCookie;
  private final String realmHandleContextKey;

  /**
   * oauth callback comes with a code and a state
   * We store the client id in a cookie for this case.
   */
  private final FrontEndCookie<String> lastAuthClientIdCookie;

  private AuthClientHandler(Config config) {

    this.apiApp = config.apiApp;
    this.realmHandleContextKey = config.realmHandleContextKey;

    String cookieName = this.apiApp.getApexDomain().getPrefixName() + "-auth-realm";
    this.lastAuthRealmCookie = FrontEndCookie.conf(cookieName, Realm.class)
      .setPath("/") // send back from all pages
      .setJsonMapper(this.apiApp.getRealmProvider().getPublicJsonMapper())
      .build();

    String clientIdCookieName = this.apiApp.getApexDomain().getPrefixName() + "-auth-client-id-last";
    this.lastAuthClientIdCookie = FrontEndCookie.conf(clientIdCookieName, String.class)
      .setPath("/") // send back from all pages
      .build();

  }

  public static Config config(EraldyApiApp eraldyApiApp) {
    return new Config(eraldyApiApp);
  }


  private String getClientId(RoutingContext routingContext) throws NotFoundException {


    HttpServerRequest request = routingContext.request();

    /**
     * From Query String
     */
    String clientId = request.getParam(CLIENT_ID);
    if (clientId != null) {
      return clientId;
    }

    /**
     * From header
     */
    clientId = request.getHeader(X_CLIENT_ID);
    if (clientId != null) {
      return clientId;
    }

    /**
     * Oauth special case
     * The oauth callbacks have only code and state.
     * https://localhost:8083/auth/oauth/github/callback?code=xxx&state=xxx
     * To keep the clientId, we may use the last cookie
     */
    if (request.path().matches(".*oauth.*callback")) {
      try {
        return this.lastAuthClientIdCookie.getValue(routingContext);
      } catch (NullValueException e) {
        // should not
        throw new InternalException("The last client id cookie should have been set for oauth");
      } catch (CastException e) {
        throw new InternalException("The last client id cookie could not be read", e);
      }
    }

    /**
     * Fail
     */
    throw new NotFoundException();


  }


  @Override
  public void handle(RoutingContext context) {


    Future<AuthClient> futureAuthClient;
    try {
      futureAuthClient = getFutureAuthClient(context);
    } catch (NotFoundException e) {
      /**
       * Fail
       */
      TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The client id is mandatory and was not found")
        .buildWithContextFailingTerminal(context);
      return;
    }

    futureAuthClient
      .onFailure(e -> TowerFailureException
        .builder()
        .setMessage("Internal error: the client id could not be retrieved")
        .setCauseException(e)
        .buildWithContextFailingTerminal(context)
      )
      .onSuccess(apiClient -> {

          if (apiClient == null) {
            /**
             * The client is mandatory for the authentication by realm via cookie
             * Even if we get an app or list guid that contains the realm,
             * it's too difficult to manage the {@link EraldySessionHandler session}
             * at the api implementation (too late in the calls chain)
             */
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage("The client id is mandatory and was not found")
              .buildWithContextFailingTerminal(context);
            context.next();
            return;
          }

          /**
           * We set the realm handle for the creation of the session cookie name
           * in the session handler.
           */
          Realm realm = apiClient.getApp().getRealm();
          context.put(this.realmHandleContextKey, realm.getHandle().toLowerCase());

          /**
           * We set the last client id for oauth
           */
          this.lastAuthClientIdCookie.setValue(apiClient.getGuid(), context);

          /**
           * We set the realm in a cookie for the frontend.
           * They read and get the realm this way for now.
           */
          lastAuthRealmCookie.setValue(realm, context);

          /**
           * To retrieve the request client quickly
           */
          context.put(CLIENT_ID_CONTEXT_KEY, apiClient);
          context.next();

        }
      );

  }

  /**
   * Try to find a client id and return a future.
   * <p>
   * Special Case: If the client id is not found, we check to see if the user is not already
   * set by the {@link HttpServer#getApiKeyAuthHandler()} handler.
   * @throws NotFoundException if none is found
   */
  private Future<AuthClient> getFutureAuthClient(RoutingContext context) throws NotFoundException {

    String clientId;
    try {
      clientId = getClientId(context);
      return this.apiApp
        .getAuthClientProvider()
        .getClientFromClientId(clientId);
    } catch (NotFoundException e) {

      /**
       * Already logged user via X-Api-Key?
       */
      User user = context.user();
      if (user != null) {
        Set<Authorization> authorization = user.authorizations().get(API_KEY_PROVIDER_ID);
        if (authorization != null && authorization.contains(ROOT_AUTHORIZATION)) {
          return Future.succeededFuture(this.apiApp.getAuthClientProvider().getApiKeyRootClient());
        }
      }

    }
    throw new NotFoundException();


  }

  /**
   * @param routingContext - the routing context
   * @return the realm or throw
   */
  public AuthClient getApiClientStoredOnContext(RoutingContext routingContext) {
    AuthClient authClient = routingContext.get(AuthClientHandler.CLIENT_ID_CONTEXT_KEY);
    if (authClient == null) {
      throw new InternalException("The authentication realm was not found, does the auth realm handler was executed before in the route hierarchy");
    }
    return authClient;
  }


  public static class Config {
    private final EraldyApiApp apiApp;
    private String realmHandleContextKey;

    public Config(EraldyApiApp eraldyApiApp) {
      this.apiApp = eraldyApiApp;
    }

    public Config setRealmHandleContextKey(String contextKey) {
      this.realmHandleContextKey = contextKey;
      return this;
    }

    public AuthClientHandler build() {
      return new AuthClientHandler(this);
    }
  }
}
