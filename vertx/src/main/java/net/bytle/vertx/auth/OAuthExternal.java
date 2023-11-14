package net.bytle.vertx.auth;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.TowerApp;

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


  private final Map<String, OAuthExternalProvider> OAUTH_PROVIDERS = new HashMap<>();
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

  private final TowerApp towerApp;
  private final String pathMount;


  public OAuthExternal(TowerApp towerApp, String pathMount) {
    this.towerApp = towerApp;
    this.pathMount = pathMount;
  }

  public OAuthExternal addExternal(String provider, Router router) {

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
    OAuthExternalProvider oauthExternalProvider;
    switch (provider) {
      case OAuthExternalGithub.GITHUB_TENANT:
        oauthExternalProvider = new OAuthExternalGithub(this, clientId, clientSecret);
        break;
      case OAuthExternalGoogle.GOOGLE_TENANT:
        oauthExternalProvider = new OAuthExternalGoogle(this, clientId, clientSecret);
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
    oauthExternalProvider.addCallBackHandlers(router);

    /**
     * Add the provider
     */
    this.OAUTH_PROVIDERS.put(provider, oauthExternalProvider);

    return this;

  }




  public OAuthExternalProvider getProvider(String oauthProvider) throws NotFoundException {
    OAuthExternalProvider oauth = OAUTH_PROVIDERS.get(oauthProvider);
    if (oauth == null) {
      throw new NotFoundException("No provider found with the name (" + oauthProvider + ")");
    }
    return oauth;
  }

  public TowerApp getTowerApp() {
    return this.towerApp;
  }

  public String getPathMount() {
    return this.pathMount;
  }


}
