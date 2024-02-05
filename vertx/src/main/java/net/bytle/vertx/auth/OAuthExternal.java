package net.bytle.vertx.auth;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.oauth2.authorization.ScopeAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.type.Casts;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.TowerApp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

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

  static Logger LOGGER = LogManager.getLogger(OAuthExternal.class);

  private final Map<OAuthExternalIdentifier, OAuthExternalProvider> OAUTH_PROVIDERS = new HashMap<>();
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

  private final OAuthExternalCodeFlow flow;
  private final String pathMount;


  public OAuthExternal(OAuthExternalCodeFlow flow, String pathMount) throws ConfigIllegalException {
    this.flow = flow;
    this.pathMount = pathMount;
    addExternalProvider(OAuthExternalIdentifier.GITHUB);
    addExternalProvider(OAuthExternalIdentifier.GOOGLE);
  }

  private OAuthExternal addExternalProvider(OAuthExternalIdentifier provider) throws ConfigIllegalException {

    /**
     * Auth Provider
     */
    TowerApp app = flow.getApp();
    String clientIdConf = app.getAppConfName() + ".oauth." + provider + ".client.id";
    ConfigAccessor configAccessor = app.getApexDomain().getHttpServer().getServer().getConfigAccessor();
    String clientId = configAccessor.getString(clientIdConf);
    if (clientId == null) {
      throw new ConfigIllegalException("The client id configuration (" + clientIdConf + ") was not found");
    }
    String clientSecretKey = app.getAppConfName() + ".oauth." + provider + ".client.secret";
    String clientSecret = configAccessor.getString(clientSecretKey);
    if (clientSecret == null) {
      throw new ConfigIllegalException("The client secret configuration (" + clientSecretKey + ") was not found");
    }
    OAuthExternalProvider oauthExternalProvider;
    switch (provider) {
      case GITHUB:
        oauthExternalProvider = new OAuthExternalGithub(this, clientId, clientSecret);
        break;
      case GOOGLE:
        oauthExternalProvider = new OAuthExternalGoogle(this, clientId, clientSecret);
        break;
      default:
        throw new ConfigIllegalException("The OAuth provider (" + provider + ") is unknown.");
    }


    /**
     * Add the provider
     */
    this.OAUTH_PROVIDERS.put(provider, oauthExternalProvider);

    return this;

  }

  /**
   * Add dynamically the callback handler
   */
  public void addCallBackHandlers(Router router) {

    for (OAuthExternalProvider authExternalProvider : this.OAUTH_PROVIDERS.values()) {
      String callbackLocalRouterPath = authExternalProvider.getCallbackOperationPath();
      router.route(callbackLocalRouterPath)
        .method(HttpMethod.GET)
        .handler(new OAuthExternalCallbackHandler(authExternalProvider))
        .handler(
          // Check authorization
          AuthorizationHandler
            .create(PermissionBasedAuthorization.create(OAuthExternalGithub.USER_EMAIL_SCOPE))
            .addAuthorizationProvider(ScopeAuthorization.create(" "))
        );
      LOGGER.info("Oauth Callback for provider (" + authExternalProvider + ") added at (" + flow.getApp().getOperationUriForLocalhost(callbackLocalRouterPath) + " , " + flow.getApp().getOperationUriForPublicHost(callbackLocalRouterPath) + ")");
    }
  }


  public OAuthExternalProvider getProvider(String oauthProvider) throws NotFoundException {
    OAuthExternalIdentifier oauthIdentifier;
    try {
      oauthIdentifier = Casts.cast(oauthProvider, OAuthExternalIdentifier.class);
    } catch (CastException e) {
      throw new NotFoundException("No provider found with the name (" + oauthProvider + ")");
    }
    OAuthExternalProvider oauth = OAUTH_PROVIDERS.get(oauthIdentifier);
    if (oauth == null) {
      throw new NotFoundException("No provider found with the name (" + oauthProvider + ")");
    }
    return oauth;
  }


  public String getPathMount() {
    return this.pathMount;
  }


  public OAuthExternalCodeFlow getFlow() {
    return this.flow;
  }

}
