package net.bytle.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.vertx.TowerApp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.bytle.vertx.auth.OAuthExternal.PKCE_SESSION_KEY;
import static net.bytle.vertx.auth.OAuthExternal.STATE_SESSION_KEY;

public abstract class OAuthExternalProviderAbs implements OAuthExternalProvider {

  static Logger LOGGER = LogManager.getLogger(OAuthExternalProviderAbs.class);
  private final OAuth2Auth authProvider;
  /**
   * <a href="https://vertx.io/docs/vertx-auth-common/java/#_sharing_pseudo_random_number_generator">Check documentation</a>
   */
  private final VertxContextPRNG prng;

  // Extra security layer Proof Key for Code Exchange. PKCE
  // https://how-to.vertx.io/web-and-oauth2-oidc/#why-persistence-is-important
  private int pkce = -1;
  private final TowerApp towerApp;
  private final OAuthExternal oAuthExternal;

  public OAuthExternalProviderAbs(OAuthExternal oAuthExternal, OAuth2Auth authProvider) {

    this.authProvider = authProvider;
    towerApp = oAuthExternal.getTowerApp();
    this.oAuthExternal = oAuthExternal;
    this.prng = VertxContextPRNG.current(towerApp.getApexDomain().getHttpServer().getServer().getVertx());
    LOGGER.info("The OAuth provider (" + this + ") was added with the callback URL: " + this.getCallbackOperationPath());

  }

  // Extra security layer Proof Key for Code Exchange. PKCE
  // https://how-to.vertx.io/web-and-oauth2-oidc/#why-persistence-is-important
  @SuppressWarnings("unused")
  public OAuthExternalProviderAbs pkceVerifierLength(int length) {
    if (length >= 0) {
      // requires verification
      if (length < 43 || length > 128) {
        throw new IllegalArgumentException("Length must be between 34 and 128");
      }
    }
    this.pkce = length;
    return this;
  }


  @Override
  public void authenticate(Oauth2Credentials oAuthCodeCredentials, Handler<AsyncResult<User>> resultHandler) {
    this.authProvider.authenticate(oAuthCodeCredentials, resultHandler);
  }

  @Override
  public Future<JsonObject> userInfo(User oAuthUser) {
    return this.authProvider.userInfo(oAuthUser);
  }

  @Override
  public String getAuthorizeUrl(RoutingContext context, String listGuid) {

    final Session session = context.session();

    if (session == null) {
      throw new IllegalStateException("A session is required");
    }

    // Store the state in the session
    String state = createState(listGuid).toUrlValue();
    session.put(STATE_SESSION_KEY, state);

    // Pkce
    if (pkce > 0) {
      String codeVerifier = prng.nextString(pkce);
      // store the code verifier in the session
      session.put(PKCE_SESSION_KEY, codeVerifier);
    }

    OAuth2AuthorizationURL authorizationURL = new
      OAuth2AuthorizationURL()
      .setRedirectUri(this.getCallbackPublicUri())
      .setState(state)
      .setScopes(getRequestedScopes());

    return this.authProvider.authorizeURL(authorizationURL);
  }

  /**
   * Create a state to mitigate replay attacks and add state
   *
   * @param listGuid - the list guid where to register
   * @return the state with a random number
   */
  private AuthState createState(String listGuid) {


    String random = prng.nextString(6);
    AuthState authState = new AuthState(new JsonObject());
    authState.setRandomValue(random);
    if (listGuid != null) {
      authState.setListGuid(listGuid);
    }
    return authState;


  }

  public String getCallbackPublicUri() {
    /**
     * We follow the same path as in the openApi file
     * The callback is saved hard core in the setting of GitHub
     */
    String providerCallbackOperationPath = this.getCallbackOperationPath();
    return towerApp.getOperationUriForPublicHost(providerCallbackOperationPath).toUri().toString();
  }

  @Override
  public String getCallbackOperationPath() {
    return towerApp.getPathMount() + oAuthExternal.getPathMount() + "/" + this.getName() + "/callback";
  }

  @Override
  public OAuthExternal getOAuthExternal() {
    return this.oAuthExternal;
  }

  @Override
  public String toString() {
    return this.getName();
  }

}
