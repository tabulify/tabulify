package net.bytle.tower.util;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.auth.oauth2.authorization.ScopeAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.RegistrationFlow;
import net.bytle.tower.eraldy.model.openapi.RegistrationList;
import net.bytle.type.time.Date;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.HttpRequestUtil;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.TowerApp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The handling of an external OAuth authentication
 * <p>
 * Adaptation of {@link OAuth2AuthHandler} to our flow
 * <a href="https://vertx.io/docs/vertx-auth-oauth2/java/">...</a>
 * <p>
 * Examples:
 * <a href="https://how-to.vertx.io/web-and-oauth2-oidc/">...</a>
 * <a href="https://github.com/eclipse-vertx/vertx-auth/blob/master/vertx-auth-oauth2/src/main/java/examples/AuthOAuth2Examples.java">...</a>
 */
public class OAuthExternal {


  private static final Logger LOGGER = LogManager.getLogger(OAuthExternal.class);

  public static final String GITHUB_TENANT = "github";
  public static final String GOOGLE_TENANT = "google";
  public static final String ROOT_AUTH_PATH = "/oauth";
  private static final Map<String, OAuthExternal> OAUTH_PROVIDERS = new HashMap<>();
  /**
   * A prefix to avoid name collision
   */
  private static final String PREFIX = "auth-external-";
  public static final String PKCE_SESSION_KEY = PREFIX + "pkce";

  /**
   * The state send out to an external oauth provider
   * and saved into the session
   */
  public static final String STATE_SESSION_KEY = PREFIX + "state";


  private final OAuthExternalProvider oauthExternalProvider;
  private final EraldyApiApp apiApp;
  private final String provider;

  // Extra security layer Proof Key for Code Exchange. PKCE
  // https://how-to.vertx.io/web-and-oauth2-oidc/#why-persistence-is-important
  private final int pkce;

  /**
   * <a href="https://vertx.io/docs/vertx-auth-common/java/#_sharing_pseudo_random_number_generator">Check documentation</a>
   */
  private final VertxContextPRNG prng;


  public OAuthExternal(TowerApp towerApp, String provider, Router router, int pkce) {

    this.apiApp = (EraldyApiApp) towerApp;
    this.provider = provider;
    this.pkce = pkce;
    this.prng = VertxContextPRNG.current(towerApp.getApexDomain().getHttpServer().getServer().getVertx());

    /**
     * Auth Provider
     */
    String clientIdConf = towerApp.getAppConfName() + ".oauth." + provider + ".client.id";
    ConfigAccessor configAccessor = towerApp.getApexDomain().getHttpServer().getServer().getConfigAccessor();
    String clientId = configAccessor.getString(clientIdConf);
    if (clientId == null) {
      throw new InternalException("The client id configuration (" + clientIdConf + ") was not found");
    }
    String clientSecretKey = towerApp.getAppConfName() + ".oauth." + provider + ".client.secret";
    String clientSecret = configAccessor.getString(clientSecretKey);
    if (clientSecret == null) {
      throw new InternalException("The client secret configuration (" + clientSecretKey + ") was not found");
    }
    switch (provider) {
      case GITHUB_TENANT:
        oauthExternalProvider = new OAuthExternalGithub(towerApp, clientId, clientSecret);
        break;
      case GOOGLE_TENANT:
        oauthExternalProvider = new OAuthExternalGoogle(towerApp, clientId, clientSecret);
        break;
      default:
        throw new IllegalArgumentException("The OAuth provider (" + provider + ") is unknown.");
    }

    /**
     * Callback route handler
     * We add it here because
     * the callback public url method is a shared data between callback and authorization
     * and the method is then shared in this object
     * {@link #getCallbackPublicRedirectUri()}
     */
    addCallBackHandler(router);

  }

  /**
   * Add dynamically the callback handler
   */
  private void addCallBackHandler(Router router) {


    String callbackLocalRouterPath = apiApp.getPathMount() + this.getCallbackOperationPath();
    router.route(callbackLocalRouterPath)
      .method(HttpMethod.GET)
      .handler(AuthOAuthCallbackHandler
        .create(this)
      )
      .handler(
        // Check authorization
        AuthorizationHandler
          .create(PermissionBasedAuthorization.create(OAuthExternalGithub.USER_EMAIL_SCOPE))
          .addAuthorizationProvider(ScopeAuthorization.create(" "))
      );

  }


  public static OAuthExternal get(String oauthProvider) throws NotFoundException {
    OAuthExternal oauth = OAUTH_PROVIDERS.get(oauthProvider);
    if (oauth == null) {
      throw new NotFoundException("No provider found with the name (" + oauthProvider + ")");
    }
    return oauth;
  }

  public static void build(TowerApp towerApp, Router router) {

    OAuthExternal githubOAuth = OAuthExternal.config(towerApp, router, OAuthExternal.GITHUB_TENANT).build();
    OAUTH_PROVIDERS.put(OAuthExternal.GITHUB_TENANT, githubOAuth);
    OAuthExternal googleOAuth = OAuthExternal.config(towerApp, router, OAuthExternal.GOOGLE_TENANT).build();
    OAUTH_PROVIDERS.put(OAuthExternal.GOOGLE_TENANT, googleOAuth);

  }

  private static Config config(TowerApp towerApp, Router router, String provider) {
    return new Config(towerApp, router, provider);
  }


  public Future<String> getAuthorizeUrl(RoutingContext context, String listGuid) {

    final Session session = context.session();

    if (session == null) {
      throw new IllegalStateException("A session is required");
    }


    return createState(context, listGuid)
      .compose(oAuthExternalState -> {

          // Store the state in the session
          String state = oAuthExternalState.toStateString();
          session.put(STATE_SESSION_KEY, state);

          // Pkce
          if (pkce > 0) {
            String codeVerifier = prng.nextString(pkce);
            // store the code verifier in the session
            session.put(PKCE_SESSION_KEY, codeVerifier);
          }

          OAuth2AuthorizationURL authorizationURL = new
            OAuth2AuthorizationURL()
            .setRedirectUri(this.getCallbackPublicRedirectUri())
            .setState(state);

          return Future.succeededFuture(oauthExternalProvider.authorizeURL(authorizationURL));
        }
      );


  }

  /**
   * Create a state to mitigate replay attacks and add state
   *
   * @param context  - the context
   * @param listGuid - the list guid where to register
   * @return the state with a random number
   */
  private Future<OAuthExternalState> createState(RoutingContext context, String listGuid) {


    Future<RegistrationList> listFuture;
    if (listGuid != null) {
      listFuture = apiApp.getListProvider()
        .getListByGuid(listGuid);
    } else {
      listFuture = Future.succeededFuture();
    }

    return listFuture.compose(listFromFuture -> {
      if (listGuid != null) {
        if (listFromFuture == null) {
          return Future.failedFuture(new NotFoundException("The list (" + listGuid + ") is unknown"));
        }
        Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(context);
        if (!(authRealm.getLocalId().equals(listFromFuture.getRealm().getLocalId()))) {
          return Future.failedFuture(new IllegalArgumentException("The realm is not the same for the list (Handle: " + listFromFuture.getHandle() + ", Realm Handle: " + listFromFuture.getRealm().getHandle() + ") and for the authentication realm (" + authRealm.getHandle() + ")"));
        }
      }
      String random = prng.nextString(6);
      OAuthExternalState oAuthExternalState = new OAuthExternalState();
      oAuthExternalState.setRandomValue(random);
      oAuthExternalState.setListGuid(listGuid);
      return Future.succeededFuture(oAuthExternalState);
    });

  }

  private String getCallbackPublicRedirectUri() {
    /**
     * We follow the same path as in the openApi file
     * The callback is saved hard core in the setting of GitHub
     * It should be in dev `member.combostrap.local:8083`
     */
    String providerCallbackOperationPath = this.getCallbackOperationPath();
    String callbackPublicURL = apiApp.getOperationUriForPublicHost(providerCallbackOperationPath).toUri().toString();
    LOGGER.info("The calculated callback URL for the provider (" + provider + ") is " + callbackPublicURL);
    return callbackPublicURL;
  }

  private String getCallbackOperationPath() {
    return ROOT_AUTH_PATH + "/" + provider + "/callback";
  }

  public static class Config {

    private final Router router;
    private final String provider;
    private final TowerApp towerApp;
    private int pkce = -1;

    public Config(TowerApp towerApp, Router router, String provider) {
      this.provider = provider;
      this.router = router;
      this.towerApp = towerApp;
    }

    // Extra security layer Proof Key for Code Exchange. PKCE
    // https://how-to.vertx.io/web-and-oauth2-oidc/#why-persistence-is-important
    @SuppressWarnings("unused")
    public Config pkceVerifierLength(int length) {
      if (length >= 0) {
        // requires verification
        if (length < 43 || length > 128) {
          throw new IllegalArgumentException("Length must be between 34 and 128");
        }
      }
      this.pkce = length;
      return this;
    }

    public OAuthExternal build() {

      return new OAuthExternal(towerApp, provider, router, pkce);

    }

  }

  /**
   * The Oauth callback handler
   * (Adapted from {@link io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl}
   * <p>
   * It needs to implement {@link AuthenticationHandler}
   * to get the good VALIDATION order (<a href="https://vertx.io/blog/whats-new-in-vert-x-4-3/#vertx-web">order</a>)
   */
  private static class AuthOAuthCallbackHandler implements AuthenticationHandler {


    private final OAuthExternal oAuthExternal;

    public AuthOAuthCallbackHandler(OAuthExternal oAuthExternal) {
      this.oAuthExternal = oAuthExternal;
    }

    public static AuthOAuthCallbackHandler create(OAuthExternal authProvider) {
      return new AuthOAuthCallbackHandler(authProvider);
    }

    @Override
    public void handle(RoutingContext ctx) {
      /**
       * Error URL
       * <p>
       * Example GitHub:
       * ```
       * callback?
       * error=redirect_uri_mismatch&
       * error_description=The+redirect_uri+MUST+match+the+registered+callback+URL+for+this+application.
       * &error_uri=https%3A%2F%2Fdocs.github.com%2Fapps%2Fmanaging-oauth-apps%2Ftroubleshooting-authorization-request-errors%2F%23redirect-uri-mismatch
       * &state=qbAE3KFQ
       * <p>
       * IdP's (e.g.: AWS Cognito, Github) returns errors as query arguments
       */
      String error = ctx.request().getParam("error");
      if (error != null) {
        int errorCode;
        // standard error's from the Oauth2 RFC
        switch (error) {
          case "invalid_token":
            errorCode = 401;
            break;
          case "insufficient_scope":
            errorCode = 403;
            break;
          case "invalid_request":
          case "redirect_uri_mismatch": // github
          default:
            errorCode = 400;
            break;
        }

        String errorDescription = ctx.request().getParam("error_description");
        if (errorDescription != null) {
          ctx.fail(errorCode, new IllegalStateException(error + ": " + errorDescription));
        } else {
          ctx.fail(errorCode, new IllegalStateException(error));
        }
        return;
      }

      // Handle the callback of the flow
      final String code = ctx.request().getParam(OAuthQueryProperty.CODE.toString());

      // code is a require value
      if (code == null) {
        ctx.fail(400, new IllegalStateException("Missing code parameter"));
        return;
      }

      final Oauth2Credentials oAuthCodeCredentials = new Oauth2Credentials()
        .setFlow(OAuth2FlowType.AUTH_CODE)
        .setCode(code);

      // the state that was passed to the IdP server. The state can be
      // an opaque random string (to protect against replay attacks)
      // or if there was no session available the target resource to
      // server after validation
      final String state = ctx.request().getParam(OAuthQueryProperty.STATE.toString());
      if (state == null) {
        ctx.fail(400, new IllegalStateException("Missing IdP state parameter to the callback endpoint"));
        return;
      }


      final Session session = ctx.session();
      String ctxState = session.remove(STATE_SESSION_KEY);
      if (!state.equals(ctxState)) {
        // forbidden, the state is not valid (replay attack?)
        ctx.fail(401, new IllegalStateException("Invalid oauth2 state"));
        return;
      }
      OAuthExternalState oAuthExternalState = OAuthExternalState.createFromStateString(ctxState);
      String listGuid = oAuthExternalState.getListGuid();

      // remove the code verifier, from the session as it will be trade for the
      // token during the final leg of the oauth2 handshake
      String codeVerifier = session.remove(PKCE_SESSION_KEY);
      oAuthCodeCredentials.setCodeVerifier(codeVerifier);


      // The valid callback URL set in your IdP application settings.
      // This must exactly match the redirect_uri passed to the authorization URL in the previous step.
      oAuthCodeCredentials.setRedirectUri(oAuthExternal.getCallbackPublicRedirectUri());

      /**
       * Get the access token with the code
       * <p>
       * With [GitHub](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authenticating-to-the-rest-api-with-an-oauth-app#providing-a-callback),
       * post call to `https://github.com/login/oauth/access_token`
       */
      OAuthExternalProvider oAuthProvider = oAuthExternal.getOAuthProvider();
      oAuthProvider.authenticate(oAuthCodeCredentials, res -> {
        if (res.failed()) {
          ctx.fail(res.cause());
          return;
        }
        /**
         * User is a json that has the following principal properties:
         * * `access_token`
         * * `token_type` (value: `bearer`)
         * * `scope` (value: `user:email` as asked)
         */
        User oAuthUser = res.result();

        /**
         * Users can edit the required scopes
         * We need to check if we were granted `user:email` scope
         */
        String scope = oAuthUser.get("scope");
        if (scope == null) {
          ctx.fail(new InternalException("The scope value was null"));
          return;
        }
        /**
         * Example: google
         * `https://www.googleapis.com/auth/userinfo.profile openid https://www.googleapis.com/auth/userinfo.email`
         */
        Set<String> responsesScopes = Arrays.stream(scope.split(" "))
          .map(String::trim)
          .collect(Collectors.toSet());
        HttpServerResponse response = ctx.response();
        List<String> requestedScopes = oAuthProvider.getRequestedScopes();
        for (String requestedScope : requestedScopes) {
          if (!responsesScopes.contains(requestedScope)) {
            response
              .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
              .setStatusCode(HttpStatus.NOT_AUTHORIZED)
              .end("The requested scope (" + requestedScope + ") was not granted.");
            return;
          }
        }

        /**
         * Get user info
         * the Client makes a request to the UserInfo Endpoint
         * https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
         * <p>
         * For GitHub:
         * https://api.github.com/user
         * https://docs.github.com/en/rest/users/users?apiVersion=2022-11-28#get-the-authenticated-user
         */
        String accessToken = oAuthUser.get("access_token");
        Future<JsonObject> userInfoFuture = oAuthProvider.userInfo(oAuthUser);

        /**
         * The Oauth user (an internal user, created from Oauth data)
         */
        Future<net.bytle.tower.eraldy.model.openapi.User> oauthUserFuture = userInfoFuture
          .onFailure(err -> {
            ctx.session().destroy();
            ctx.fail(err);
          })
          .compose(userInfo -> oAuthProvider.getEnrichedUser(ctx, userInfo, accessToken));

        /**
         * Retrieve the internal user from the realm and oauth user
         */
        Future<net.bytle.tower.eraldy.model.openapi.User> userFuture = oauthUserFuture
          .onFailure(err -> {
            ctx.session().destroy();
            ctx.fail(err);
          })
          .compose(oauthUser -> {
            Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(ctx);

            oauthUser.setRealm(authRealm);
            /**
             * Create our principal
             */
            return oAuthExternal.apiApp.getUserProvider()
              .createOrPatchIfNull(oauthUser);

          });

        /**
         * Authenticate and redirect
         */
        userFuture
          .onFailure(err -> {
            ctx.session().destroy();
            ctx.fail(err);
          })
          .onSuccess(userFromProvider -> {

            /**
             * A user registration: redirects to the redirect uri
             */
            if (listGuid == null) {
              AuthInternalAuthenticator.createWith(oAuthExternal.apiApp, ctx, userFromProvider)
                .redirectViaHttp()
                .authenticate();
              return;
            }

            /**
             * A list registration
             */
            Date optInTime = Date.createFromNow();
            String optInIp;
            try {
              optInIp = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
            } catch (NotFoundException e) {
              LOGGER.warn("Oauth List registration: The remote ip client could not be found. Error: " + e.getMessage());
              optInIp = "";
            }
            ListRegistrationFlow.authenticateAndRegisterUserToList(oAuthExternal.apiApp, ctx, listGuid, userFromProvider, optInTime, optInIp, RegistrationFlow.OAUTH);

          });
      });
    }
  }

  private OAuthExternalProvider getOAuthProvider() {
    return this.oauthExternalProvider;
  }


}
