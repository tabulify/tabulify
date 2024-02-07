package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.eraldy.objectProvider.AuthClientProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.FrontEndCookie;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.OAuthState;

import java.util.Set;

import static net.bytle.vertx.TowerFailureTypeEnum.BAD_REQUEST_400;
import static net.bytle.vertx.TowerFailureTypeEnum.NOT_AUTHORIZED_403;
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
 * next handlers can retrieve the realm with the function {@link #getRequestingClient(RoutingContext)}
 */
public class AuthClientHandler implements Handler<RoutingContext> {

  private static final String CLIENT_ID_CONTEXT_KEY = "client-id-context-key";

  /**
   * This is used to store the app attached to the request.
   * The client id may be our apps that proxy another app.
   * For instance, list registration has a public form that is hosted
   * by our app but the app of the request is proxied
   */
  private static final String APP_ID_CONTEXT_KEY = "app-id-context-key";

  /**
   * The client id
   */
  private static final String X_CLIENT_ID = "x-client-id";

  /**
   * The app id of the request
   */
  private static final String X_PROXY_APP_ID = "x-proxy-app-id";
  /**
   * The client id that the client wants to proxy
   * (Used in the member/auth app)
   */

  private static final String X_PROXY_CLIENT_ID = "x-proxy-client-id";

  /**
   * The client id
   */
  private static final String CLIENT_ID = "client_id";

  private final EraldyApiApp apiApp;

  /**
   * The cookie that stores the last realm
   * information (when the front end is loaded)
   */

  private final String realmGuidContextKey;
  private final String realmHandleContextKey;

  private AuthClientHandler(Config config) {

    this.apiApp = config.apiApp;
    this.realmGuidContextKey = config.realmGuidContextKey;
    this.realmHandleContextKey = config.realmHandleContextKey;

  }

  public static Config config(EraldyApiApp eraldyApiApp) {
    return new Config(eraldyApiApp);
  }


  public String getClientId(RoutingContext routingContext) throws NotFoundException {


    HttpServerRequest request = routingContext.request();

    /**
     * From header (classic)
     */
    String clientId = request.getHeader(X_CLIENT_ID);
    if (clientId != null) {
      return clientId;
    }

    /**
     * Auth Hack
     * Because a session is only a handler of a routing context
     * There is no way to create a session at the last handler context
     * because the session interface implements / depends on the {@link RoutingContext}
     * You can't then have a session object with two type of handlers.
     */
    if (!request.path().startsWith("/auth")) {
      throw new NotFoundException();
    }
    clientId = request.getParam(CLIENT_ID);
    if (clientId != null) {
      return clientId;
    }
    /**
     * In the callback state?
     */
    final String state = routingContext.request().getParam(AuthQueryProperty.STATE.toString());
    String message = "For Auth, the client id is mandatory.";
    if (state == null) {
      throw new InternalException(message);
    }
    OAuthState oAuthState;
    try {
      oAuthState = OAuthState.createFromStateString(state);
    } catch (CastException e) {
      throw new InternalException(message + " . The state is not in the good format");
    }
    clientId = oAuthState.getClientId();
    if (clientId == null) {
      throw new InternalException(message);
    }
    return clientId;
  }


  @Override
  public void handle(RoutingContext context) {

    Future<AuthClient> futureAuthClient = null;
    String clientId = null;
    AuthClientProvider authClientProvider = this.apiApp.getAuthClientProvider();
    try {
      clientId = getClientId(context);
      futureAuthClient = authClientProvider
        .getClientFromClientId(clientId);
    } catch (NotFoundException e) {

      /**
       * Already logged user via X-Api-Key?
       */
      User user = context.user();
      if (user != null) {
        Set<Authorization> authorization = user.authorizations().get(API_KEY_PROVIDER_ID);
        if (authorization != null && authorization.contains(ROOT_AUTHORIZATION)) {
          futureAuthClient = Future.succeededFuture(authClientProvider.getApiKeyRootClient());
        }
      }

    }

    if (futureAuthClient == null) {
      /**
       * We don't fail, without clientId, there is no domain session on the request
       * created by the {@link RealmSessionHandler domain session handler}
       * It's easier than to try to handle all cases where the session should not be created
       * such as openapi doc, callback oauth, ...
       * The program may create a session on a later stage.
       */
      context.next();
      return;
    }


    String finalClientId = clientId;
    futureAuthClient
      .onFailure(e -> TowerFailureException
        .builder()
        .setType(BAD_REQUEST_400)
        .setMessage("The client (" + finalClientId + ") is unknown")
        .setCauseException(e)
        .buildWithContextFailingTerminal(context)
      )
      .onSuccess(authClient -> {

          if (authClient == null) {
            /**
             * The client is mandatory for the authentication by realm via cookie
             * Even if we get an app or list guid that contains the realm,
             * it's too difficult to manage the {@link RealmSessionHandler session}
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
           * Proxy call?
           */
          Future<AuthClient> futureFinalAuthClient = Future.succeededFuture(authClient);
          String proxyClientId = context.request().getHeader(X_PROXY_CLIENT_ID);
          if (proxyClientId != null) {
            AuthClientScope proxyClient = AuthClientScope.PROXY_CLIENT;
            try {
              this.apiApp.getAuthProvider().checkClientAuthorization(authClient, proxyClient);
            } catch (NotAuthorizedException e) {
              TowerFailureException.builder()
                .setType(NOT_AUTHORIZED_403)
                .setMessage("The client (" + authClient.getGuid() + ") is not authorized to " + proxyClient.getHumanActionName())
                .buildWithContextFailingTerminal(context);
              return;
            }
            futureFinalAuthClient = authClientProvider.getClientFromClientId(proxyClientId);
          }

          futureFinalAuthClient
            .onFailure(e -> TowerFailureException
              .builder()
              .setCauseException(e)
              .buildWithContextFailingTerminal(context)
            )
            .onSuccess(finalAuthClient -> {

              if (finalAuthClient == null) {
                if (proxyClientId != null) {
                  TowerFailureException.builder()
                    .setType(NOT_AUTHORIZED_403)
                    .setMessage("The proxy client (" + proxyClientId + ") is unknown")
                    .buildWithContextFailingTerminal(context);
                } else {
                  TowerFailureException.builder()
                    .setMessage("The final client should be not null")
                    .buildWithContextFailingTerminal(context);
                }
                context.next();
                return;
              }

              /**
               * We set the realm handle and guid
               * for the creation of the session (cookie name and session data)
               * in the session handler.
               */
              Realm realm = finalAuthClient.getApp().getRealm();
              context.put(this.realmGuidContextKey, realm.getGuid());
              context.put(this.realmHandleContextKey, realm.getHandle());

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
                String cookieName = this.apiApp.getApexDomain().getPrefixName() + "-auth-" + finalAuthClient.getGuid();
                FrontEndCookie.conf(cookieName, AuthClient.class)
                  .setPath("/") // send back from all pages
                  .setJsonMapper(authClientProvider.getPublicJsonMapper())
                  .setHttpOnly(false)
                  .build()
                  .setValue(finalAuthClient, context);
              }

              /**
               * To retrieve the request client
               */
              context.put(CLIENT_ID_CONTEXT_KEY, finalAuthClient);

              /**
               * App determination
               * A cli can proxy request for another app
               * (This is the case of our apps when we register a user
               * to a list for instance)
               */

              Future<App> futureRequestApp = Future.succeededFuture(finalAuthClient.getApp());
              String proxyAppId = context.request().getHeader(X_PROXY_APP_ID);
              if (proxyAppId != null) {
                AuthClientScope proxyApp = AuthClientScope.PROXY_APP;
                try {
                  this.apiApp.getAuthProvider().checkClientAuthorization(authClient, proxyApp);
                } catch (NotAuthorizedException e) {
                  TowerFailureException.builder()
                    .setType(NOT_AUTHORIZED_403)
                    .setMessage("The client (" + authClient.getGuid() + ") is not authorized to " + proxyApp.getHumanActionName())
                    .buildWithContextFailingTerminal(context);
                  return;
                }
                AppProvider appProvider = apiApp.getAppProvider();
                Guid appGuid;
                try {
                  appGuid = appProvider.getGuidFromHash(proxyAppId);
                } catch (CastException e) {
                  TowerFailureException.builder()
                    .setType(BAD_REQUEST_400)
                    .setMessage("The proxy app guid value (" + proxyAppId + ") is not valid")
                    .buildWithContextFailingTerminal(context);
                  return;
                }
                futureRequestApp = appProvider.getAppByGuid(appGuid, null);
              }
              futureRequestApp
                .onFailure(e -> TowerFailureException
                  .builder()
                  .setMessage("Fatal Error while retrieving the requesting app")
                  .setCauseException(e)
                  .buildWithContextFailingTerminal(context)
                )
                .onSuccess(app -> {
                  if (app == null) {
                    if (proxyAppId != null) {
                      TowerFailureException.builder()
                        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
                        .setMessage("The app (" + proxyAppId + ") was not found")
                        .buildWithContextFailingTerminal(context);
                    } else {
                      TowerFailureException.builder()
                        .setMessage("The request app is null on the client (" + finalClientId + ")")
                        .buildWithContextFailingTerminal(context);
                    }
                    context.next();
                    return;
                  }

                  /**
                   * To retrieve the request client
                   */
                  context.put(APP_ID_CONTEXT_KEY, app);

                  /**
                   * Next handler
                   * to continue the chain
                   */
                  context.next();
                });

            });


        }
      );

  }


  /**
   * @param routingContext - the routing context
   * @return the realm or throw
   */
  public AuthClient getRequestingClient(RoutingContext routingContext) {
    AuthClient authClient = routingContext.get(AuthClientHandler.CLIENT_ID_CONTEXT_KEY);
    if (authClient == null) {
      throw new InternalException("The client id was not found");
    }
    return authClient;
  }

  public App getRequestingApp(RoutingContext routingContext) {
    App app = routingContext.get(AuthClientHandler.APP_ID_CONTEXT_KEY);
    if (app == null) {
      throw new InternalException("The app from the client was not found");
    }
    return app;
  }


  public static class Config {
    private final EraldyApiApp apiApp;
    private String realmGuidContextKey = "realm-guid";
    private String realmHandleContextKey = "realm-handle";

    public Config(EraldyApiApp eraldyApiApp) {
      this.apiApp = eraldyApiApp;
    }

    public Config setRealmGuidContextKey(String contextKey) {
      this.realmGuidContextKey = contextKey;
      return this;
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
