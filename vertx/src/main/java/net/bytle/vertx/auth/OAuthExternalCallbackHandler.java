package net.bytle.vertx.auth;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.AuthenticationHandler;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.type.time.Date;
import net.bytle.vertx.HttpRequestUtil;
import net.bytle.vertx.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Oauth callback handler
 * (Adapted from {@link io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl}
 * <p>
 * It needs to implement {@link AuthenticationHandler}
 * to get the good VALIDATION order (<a href="https://vertx.io/blog/whats-new-in-vert-x-4-3/#vertx-web">order</a>)
 */
class OAuthExternalCallbackHandler implements AuthenticationHandler {


  static Logger LOGGER = LogManager.getLogger(OAuthExternalCallbackHandler.class);

  private final OAuthExternalProviderAbs oAuthExternalProvider;

  public OAuthExternalCallbackHandler(OAuthExternalProviderAbs oAuthExternalProvider) {
    this.oAuthExternalProvider = oAuthExternalProvider;
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
    String ctxState = session.remove(OAuthExternal.STATE_SESSION_KEY);
    if (!state.equals(ctxState)) {
      // forbidden, the state is not valid (replay attack?)
      ctx.fail(401, new IllegalStateException("Invalid oauth2 state"));
      return;
    }
    OAuthExternalState oAuthExternalState = OAuthExternalState.createFromStateString(ctxState);
    String listGuid = oAuthExternalState.getListGuid();

    // remove the code verifier, from the session as it will be trade for the
    // token during the final leg of the oauth2 handshake
    String codeVerifier = session.remove(OAuthExternal.PKCE_SESSION_KEY);
    oAuthCodeCredentials.setCodeVerifier(codeVerifier);


    // The valid callback URL set in your IdP application settings.
    // This must exactly match the redirect_uri passed to the authorization URL in the previous step.
    oAuthCodeCredentials.setRedirectUri(oAuthExternalProvider.getCallbackPublicRedirectUri());

    /**
     * Get the access token with the code
     * <p>
     * With [GitHub](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authenticating-to-the-rest-api-with-an-oauth-app#providing-a-callback),
     * post call to `https://github.com/login/oauth/access_token`
     */
    oAuthExternalProvider.authenticate(oAuthCodeCredentials, res -> {
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
      List<String> requestedScopes = oAuthExternalProvider.getRequestedScopes();
      for (String requestedScope : requestedScopes) {
        if (!responsesScopes.contains(requestedScope)) {
          response
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .setStatusCode(HttpStatus.NOT_AUTHORIZED.httpStatusCode())
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
      Future<JsonObject> userInfoFuture = oAuthExternalProvider.userInfo(oAuthUser);

      /**
       * The Oauth user (an internal user, created from Oauth data)
       */
      Future<AuthUser> oauthUserFuture = userInfoFuture
        .onFailure(err -> {
          ctx.session().destroy();
          ctx.fail(err);
        })
        .compose(userInfo -> oAuthExternalProvider.getEnrichedUser(ctx, userInfo, accessToken));

//        /**
//         * Retrieve the internal user from the realm and oauth user
//         */
//        Future<net.bytle.tower.eraldy.model.openapi.User> userFuture = oauthUserFuture
//          .onFailure(err -> {
//            ctx.session().destroy();
//            ctx.fail(err);
//          })
//          .compose(oauthUser -> {
//            Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(ctx);
//
//            oauthUser.setRealm(authRealm);
//            /**
//             * Create our principal
//             */
//            return oAuthExternal.apiApp.getUserProvider()
//              .createOrPatchIfNull(oauthUser);
//
//          });

      /**
       * Authenticate and redirect
       */
      oauthUserFuture
        .onFailure(err -> {
          ctx.session().destroy();
          ctx.fail(err);
        })
        .onSuccess(authUser -> {

          /**
           * A user registration: redirects to the redirect uri
           */
          if (listGuid == null) {
            AuthInternalAuthenticator.createWith(oAuthExternalProvider.getApp(), ctx, authUser)
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
          //ListRegistrationFlow.authenticateAndRegisterUserToList(oAuthExternal.apiApp, ctx, listGuid, authUser, optInTime, optInIp, RegistrationFlow.OAUTH);

        });
    });
  }

}
