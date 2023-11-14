package net.bytle.vertx.auth;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.HttpStatusEnum;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.VertxRoutingFailureData;
import net.bytle.vertx.flow.WebFlowAbs;

/**
 * A flow with external IDP (identity provider)
 * that implements the oauth code flow
 */
public class OAuthExternalCodeFlow extends WebFlowAbs {

  private final OAuthExternal oauthExternal;

  public OAuthExternalCodeFlow(TowerApp towerApp, String pathMount) {
    super(towerApp);
    this.oauthExternal = new OAuthExternal(towerApp, pathMount);
  }

  /**
   * Because, in the open api, the order of parameter may change, we do not rely on them,
   * we get the value from the context instead
   * <p>
   * We take the value of the parameters from the context
   * because if there is a switch on position in the open api specification,
   * the data are still string and the api does not cry even if the data value received
   * is not the good one
   *
   * @param routingContext - the routing context
   * @return the redirect Uri
   * @throws NotFoundException        - if not found
   * @throws IllegalArgumentException - if it's not a URL string
   */
  public static UriEnhanced getRedirectUri(RoutingContext routingContext) throws NotFoundException {
    String redirectUri = routingContext.request().getParam(AuthQueryProperty.REDIRECT_URI.toString());
    if (redirectUri == null) {
      throw new NotFoundException();
    }
    try {
      return UriEnhanced.createFromString(redirectUri);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri is not a valid URI", AuthQueryProperty.REDIRECT_URI.toString(), redirectUri);
    }
  }

  public void step2AddProviderAndCallbacks(Router router) {
    oauthExternal
      .addExternal(OAuthExternalGithub.GITHUB_TENANT, router)
      .addExternal(OAuthExternalGoogle.GOOGLE_TENANT, router);
  }

  /**
   * @param routingContext - the context
   * @param provider       - the provider string
   * @return redirect to the Oauth provider
   */
  public Future<Void> step1RedirectToExternalIdentityProvider(RoutingContext routingContext, String provider) {

    /**
     * We don't rely on the argument because they can change of positions on the signature unfortunately
     * or in the openapi definition
     */
    String listGuid = routingContext.request().getParam(AuthQueryProperty.LIST_GUID.toString());

    /**
     * CallBack is mandatory
     *
     */
    UriEnhanced redirectUriAsUri;
    try {
      redirectUriAsUri = getRedirectUri(routingContext);
    } catch (NotFoundException e) {
      /**
       * The redirect uri is mandatory
       * For a user registration, it should be the calling application
       * For a list registration, it should be the confirmation page
       */
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatusEnum.BAD_REQUEST_400)
          .setDescription("The (" + OAuthInternalSession.REDIRECT_URI_KEY + ") of the client cannot be null for a user registration.")
          .failContext(routingContext)
          .getFailedException()
      );
    }


    OAuthInternalSession.addRedirectUri(routingContext, redirectUriAsUri);

    /**
     * Auth Realm is mandatory
     * To be sure that we have the good realm
     * in {@link AuthRealmHandler#getAuthRealmCookie(RoutingContext)}
     */
    String realmIdentifier = routingContext.request().getParam(AuthQueryProperty.REALM_IDENTIFIER.toString());
    if (realmIdentifier == null) {
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatusEnum.BAD_REQUEST_400)
          .setDescription("A realm query property identifier (" + AuthQueryProperty.REALM_IDENTIFIER + ") is mandatory.")
          .failContext(routingContext)
          .getFailedException()
      );
    }


    /**
     * Create the Oauth URL of the provider
     * and redirect to it
     */
    OAuthExternalProvider oAuthExternalProvider;
    try {
      oAuthExternalProvider = this.oauthExternal.getProvider(provider);
    } catch (NotFoundException e) {
      return Future.failedFuture(IllegalArgumentExceptions.createWithInputNameAndValue("The OAuth provider (" + provider + ") is unknown", "provider", provider));
    }
    String redirectUrl = oAuthExternalProvider
      .getAuthorizeUrl(routingContext, listGuid);
    routingContext.redirect(redirectUrl);
    return Future.succeededFuture();

  }

}
