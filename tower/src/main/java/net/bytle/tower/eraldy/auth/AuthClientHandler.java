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
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.FrontEndCookie;
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

  private static final String X_CLIENT_ID = "x-client-id";

  /**
   * The client id
   */
  private static final String CLIENT_ID = "client_id";

  private final EraldyApiApp apiApp;

  /**
   * The cookie that stores the last realm
   * information (when the front end is loaded)
   */

  private final String realmHandleContextKey;

  /**
   * oauth callback comes with a code and a state
   * We store the client id in a cookie for this case.
   */
  private final FrontEndCookie<String> lastAuthClientIdCookie;

  private AuthClientHandler(Config config) {

    this.apiApp = config.apiApp;
    this.realmHandleContextKey = config.realmHandleContextKey;


    String clientIdCookieName = this.apiApp.getApexDomain().getPrefixName() + "-auth-client-id-last";
    this.lastAuthClientIdCookie = FrontEndCookie.conf(clientIdCookieName, String.class)
      .setPath("/") // send back from all pages
      .setHttpOnly(true) // only for the server
      .setSameSiteStrictInProdAndLaxInDev()
      .build();

  }

  public static Config config(EraldyApiApp eraldyApiApp) {
    return new Config(eraldyApiApp);
  }


  private String
  getClientId(RoutingContext routingContext) throws NotFoundException {


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

    Future<AuthClient> futureAuthClient = null;
    String clientId = null;
    try {
      clientId = getClientId(context);
      futureAuthClient = this.apiApp
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
          futureAuthClient = Future.succeededFuture(this.apiApp.getAuthClientProvider().getApiKeyRootClient());
        }
      }

    }

    if (futureAuthClient == null) {
      /**
       * Fail
       */
      TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The client id is mandatory and was not found. The query property '" + CLIENT_ID + ", nor the header '" + X_CLIENT_ID + "' was found")
        .buildWithContextFailingTerminal(context);
      return;
    }


    String finalClientId = clientId;
    futureAuthClient
      .onFailure(e -> TowerFailureException
        .builder()
        .setMessage("The client id could not be retrieved")
        .setCauseException(e)
        .buildWithContextFailingTerminal(context)
      )
      .onSuccess(authClient -> {

          if (authClient == null) {
            /**
             * The client is mandatory for the authentication by realm via cookie
             * Even if we get an app or list guid that contains the realm,
             * it's too difficult to manage the {@link EraldySessionHandler session}
             * at the api implementation (too late in the calls chain)
             */
            String message;
            if (finalClientId != null) {
              message = "The client for the client id (" + finalClientId + ") was not found";
            } else {
              message = "The client for the api key was not found";
            }
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage(message)
              .buildWithContextFailingTerminal(context);
            context.next();
            return;
          }

          /**
           * We set the realm handle for the creation of the session cookie name
           * in the session handler.
           */
          Realm realm = authClient.getApp().getRealm();
          context.put(this.realmHandleContextKey, realm.getHandle().toLowerCase());

          /**
           * We set the last client id for oauth
           */
          this.lastAuthClientIdCookie.setValue(authClient.getGuid(), context);

          /**
           * We set the authCli data in a cookie for the member app.
           * It read and get the context data (ie realm) for the creation of the page, this way for now
           * so that the app does not need to make a query.
           * The member app proxy all requests as if it was another app.
           */
          if (context.request().params().contains(CLIENT_ID)) {
            // client_id is normally send as HTTP headers by client
            // it's advertised in the URL only from the member app
            // it's just a trick to not send this data for all apps
            // Why? because only the  auth app have this query parameter
            String cookieName = this.apiApp.getApexDomain().getPrefixName() + "-auth-" + authClient.getGuid();
            FrontEndCookie.conf(cookieName, AuthClient.class)
              .setPath("/") // send back from all pages
              .setJsonMapper(this.apiApp.getAuthClientProvider().getPublicJsonMapper())
              .setHttpOnly(false)
              .build()
              .setValue(authClient, context);
          }

          /**
           * To retrieve the request client quickly
           */
          context.put(CLIENT_ID_CONTEXT_KEY, authClient);
          context.next();

        }
      );

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
