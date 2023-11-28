package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AuthApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.OAuthAccessTokenResponse;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;

public class AuthApiImpl implements AuthApi {

  private final EraldyApiApp apiApp;

  public AuthApiImpl(TowerApp apiApp) {
    this.apiApp = (EraldyApiApp) apiApp;
  }


  /**
   * Redirect to the external OAuth authorization end points
   *
   * @param provider       - the OAuth provider (github, ...)
   * @param routingContext - the routing context to write the OAuth state on the session
   * @param listGuid       - the list guid where to register the user (maybe null)
   */
  @Override
  public Future<ApiResponse<Void>> authLoginOauthProviderGet(RoutingContext routingContext, String provider, String redirectUri, String listGuid, String realmIdentifier) {

    /**
     * We don't rely on the argument because they can change of positions on the signature unfortunately
     * or in the openapi definition
     */
    listGuid = routingContext.request().getParam(AuthQueryProperty.LIST_GUID.toString());

    /**
     * Auth Realm is mandatory
     * To be sure that we have the good realm
     * in {@link AuthRealmHandler#getAuthRealmCookie(RoutingContext)}
     */
    realmIdentifier = routingContext.request().getParam(AuthQueryProperty.REALM_IDENTIFIER.toString());
    if (realmIdentifier == null) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("A realm query property identifier (" + AuthQueryProperty.REALM_IDENTIFIER + ") is mandatory.")
          .buildWithContextFailing(routingContext)
      );
    }

    AuthState authState = AuthState.createEmpty();
    authState.setListGuid(listGuid);
    authState.setRealmIdentifier(realmIdentifier);

    return this.apiApp
      .getOauthFlow()
      .step1RedirectToExternalIdentityProvider(
        routingContext,
        provider,
        authState
      )
      .compose(
        v -> Future.succeededFuture(new ApiResponse<>()),
        Future::failedFuture
      );


  }


  @Override
  public Future<ApiResponse<Void>> authLoginAuthorizeGet(RoutingContext routingContext, String redirectUri) {

    /**
     * Redirect
     */
    UriEnhanced redirectUriEnhanced;
    try {
      redirectUriEnhanced = OAuthExternalCodeFlow.getRedirectUri(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("A redirect uri query property (" + AuthQueryProperty.REDIRECT_URI + ") is mandatory in your url in the authorize endpoint")
          .setMimeToHtml()
          .buildWithContextFailing(routingContext)
      );
    }

    /**
     * Realm is only eraldy for now
     */
    try {
      this.utilValidateRealmFromRedirectUri(redirectUriEnhanced);
    } catch (NotAuthorizedException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .setMessage("The redirect uri (" + redirectUri + ") is unknown")
          .setMimeToHtml()
          .buildWithContextFailing(routingContext)
      );
    }

    Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(routingContext);


    try {
      AuthUser authSignedInUser = this.apiApp.getAuthProvider().getSignedInAuthUser(routingContext);
      /**
       * Signed-in and in same realm
       */
      if (authSignedInUser != null && authSignedInUser.getAudience().equals(authRealm.getGuid())) {
        routingContext.redirect(redirectUriEnhanced.toString());
        return Future.succeededFuture();
      }
    } catch (NotFoundException e) {
      //
    }

    /**
     * Not signed-in or realm different
     */
    UriEnhanced url = this.apiApp.getMemberLoginUri(redirectUri, authRealm.getHandle());
    routingContext.redirect(url.toString());
    return Future.succeededFuture();

  }

  private void utilValidateRealmFromRedirectUri(UriEnhanced redirectUriEnhanced) throws NotAuthorizedException {
    TowerApexDomain eraldyApexDomain = this.apiApp.getApexDomain();
    if (!(redirectUriEnhanced.getApexWithoutPort().equals("localhost") || redirectUriEnhanced.getApexWithoutPort().equals(eraldyApexDomain.getApexNameWithoutPort()))) {
      throw new NotAuthorizedException();
    }
  }


  @Override
  public Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, AuthEmailPost authEmailPost) {


    return this.apiApp.getUserEmailLoginFlow()
      .handleStep1SendEmail(routingContext, authEmailPost);

  }


  @Override
  public Future<ApiResponse<net.bytle.tower.eraldy.model.openapi.OAuthAccessTokenResponse>> authLoginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri) {

    OAuthAuthorization authorization;
    try {
      authorization = OAuthCodeManagement.createOrGet().getAuthorization(code);
    } catch (NotFoundException e) {
      return Future.failedFuture(new NotFoundException("The code (" + code + ") was not found"));
    }

    if (redirectUri == null) {
      return Future.failedFuture(IllegalArgumentExceptions.createWithInputNameAndValue("The redirect_uri cannot be null", "redirect_uri", null));
    }

    if (!redirectUri.equals(authorization.getRedirectUri())) {
      return Future.failedFuture(new NotFoundException("The redirect_uri is not the valid callback"));
    }

    OAuthAccessTokenResponse oAuthAccessTokenResponse = apiApp
      .getApexDomain()
      .getHttpServer()
      .getServer()
      .getJwtAuth()
      .generateOAuthAccessTokenResponseFromAuthorization(authorization, routingContext);

    /**
     * We don't know how to share object
     */
    net.bytle.tower.eraldy.model.openapi.OAuthAccessTokenResponse oauthToken = JsonObject.mapFrom(oAuthAccessTokenResponse).mapTo(net.bytle.tower.eraldy.model.openapi.OAuthAccessTokenResponse.class);
    return Future.succeededFuture(new ApiResponse<>(oauthToken));

  }


  @Override
  public Future<ApiResponse<Void>> authLoginPasswordPost(RoutingContext routingContext, PasswordCredentials passwordCredentials) {

    String password = passwordCredentials.getLoginPassword();
    if (password == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The password cannot be null", "password", null);
    }
    String handle = passwordCredentials.getLoginHandle();
    if (handle == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The handle cannot be null", "handle", null);
    }
    return this.apiApp
      .getRealmProvider()
      .getRealmFromIdentifier(passwordCredentials.getLoginRealm())
      .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
      .compose(realm -> apiApp.getAuthProvider()
        .getAuthUserForSessionByPasswordNotNull(handle, password, realm)
        .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
        .compose(authUserForSession -> {
          new AuthContext(this.apiApp, routingContext, authUserForSession, AuthState.createEmpty())
            .redirectViaClient()
            .authenticateSession();
          return Future.succeededFuture(new ApiResponse<>());
        }));

  }

  @Override
  public Future<ApiResponse<Void>> authLoginPasswordResetPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {

    utilValidateEmailIdentifierDataUtil(emailIdentifier);
    return this.apiApp.getPasswordResetFlow()
      .step1SendEmail(routingContext, emailIdentifier);


  }

  @Override
  public Future<ApiResponse<Void>> authLoginPasswordUpdatePost(RoutingContext routingContext, PasswordOnly passwordOnly) {


    return apiApp
      .getAuthProvider().getSignedInBaseUserOrFail(routingContext)
      .compose(signedInUser -> apiApp
        .getUserProvider()
        .updatePassword(signedInUser.getLocalId(), signedInUser.getRealm().getLocalId(), passwordOnly.getPassword())
        .compose(futureUser -> {
          /**
           * Because this is a POST, we can't redirect via HTTP
           * The javascript client is doing it.
           */
          return Future.succeededFuture();
        }));

  }


  /**
   * Logout delete the user from the session
   */
  @Override
  public Future<ApiResponse<Void>> authLogoutGet(RoutingContext routingContext, String redirectUri) {

    routingContext.clearUser();

    /**
     * A session may also hold a user
     */
    Session session = routingContext.session();
    if (session != null) {
      session.destroy();
    }

    /**
     * Redirect
     */
    UriEnhanced redirectUriEnhanced;
    try {
      redirectUriEnhanced = OAuthExternalCodeFlow.getRedirectUri(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("A redirect uri query property (" + AuthQueryProperty.REDIRECT_URI + ") is mandatory in your url in the logout endpoint.")
          .setMimeToHtml()
          .buildWithContextFailing(routingContext)
      );
    }

    Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(routingContext);

    String redirect = this.apiApp
      .getMemberLoginUri(redirectUriEnhanced.toString(), authRealm.getHandle())
      .toUrl()
      .toString();

    routingContext.redirect(redirect);

    return Future.succeededFuture(new ApiResponse<>());

  }

  @Override
  public Future<ApiResponse<Void>> authRegisterListListIdentifierGet(RoutingContext routingContext, String listIdentifier) {
    throw new InternalException("Not yet implemented");
  }



  @Override
  public Future<ApiResponse<Void>> authRegisterUserPost(RoutingContext routingContext, AuthEmailPost authEmailPost) {

    return this.apiApp.getUserRegistrationFlow().handleStep1SendEmail(routingContext, authEmailPost);
  }

  @Override
  public Future<ApiResponse<Void>> authRegisterListPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody) {
    return this.apiApp.getUserListRegistrationFlow().handleStep1SendingValidationEmail(routingContext, listRegistrationPostBody)
      .compose(response -> Future.succeededFuture(new ApiResponse<>()));
  }


  private void utilValidateEmailIdentifierDataUtil(EmailIdentifier emailIdentifier) {
    ValidationUtil.validateEmail(emailIdentifier.getUserEmail(), "userEmail");
    String realmIdentifier = emailIdentifier.getRealmIdentifier();
    if (realmIdentifier == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The realm identifier cannot be null.", "realmIdentifier", null);
    }
  }


}
