package net.bytle.vertx.auth;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.HttpStatusEnum;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.VertxFailureHttpException;
import net.bytle.vertx.flow.WebFlowAbs;

import java.util.List;

/**
 * A flow with external IDP (identity provider)
 * that implements the oauth code flow
 */
public class OAuthExternalCodeFlow extends WebFlowAbs {

  private final OAuthExternal oauthExternal;


  public OAuthExternalCodeFlow(TowerApp towerApp, String pathMount, List<Handler<AuthContext>> authHandlers) throws ConfigIllegalException {
    super(towerApp);
    this.oauthExternal = new OAuthExternal(towerApp, pathMount, authHandlers);
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
    oauthExternal.addCallBackHandlers(router);
  }

  /**
   * @param routingContext - the context
   * @param provider       - the provider string
   * @param authState - the auth state
   * @return redirect to the Oauth provider
   */
  public Future<Void> step1RedirectToExternalIdentityProvider(RoutingContext routingContext, String provider, AuthState authState) {


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
        VertxFailureHttpException
          .builder()
          .setStatus(HttpStatusEnum.BAD_REQUEST_400)
          .setMessage("The (" + OAuthInternalSession.REDIRECT_URI_KEY + ") of the client cannot be null for a user registration.")
          .buildWithContextFailing(routingContext)
      );
    }


    OAuthInternalSession.addRedirectUri(routingContext, redirectUriAsUri);



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
      .getAuthorizeUrl(routingContext, authState);
    routingContext.redirect(redirectUrl);
    return Future.succeededFuture();

  }

}
