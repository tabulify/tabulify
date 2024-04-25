package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AuthApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.type.Handle;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.OAuthAccessTokenResponse;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import net.bytle.vertx.jackson.JacksonMapperManager;

public class AuthApiImpl implements AuthApi {

  private final EraldyApiApp apiApp;

  public AuthApiImpl(TowerApp apiApp) {
    this.apiApp = (EraldyApiApp) apiApp;
  }


  /**
   * Redirect to the external OAuth authorization end points
   *
   * @param provider       - the OAuth provider (GitHub, ...)
   * @param routingContext - the routing context to write the OAuth state on the session
   * @param listGuid       - the list guid where to register the user (maybe null)
   */
  @Override
  public Future<ApiResponse<Void>> authLoginOauthProviderGet(RoutingContext routingContext, String provider, String redirectUri, String listGuid, String clientId, String appGuid) {


    RoutingContextWrapper routingContextWrapper = new RoutingContextWrapper(routingContext);

    redirectUri = routingContextWrapper.getRequestQueryParameterAsString(AuthQueryProperty.REDIRECT_URI.toString());
    if (redirectUri == null) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The redirect URL query property  (" + AuthQueryProperty.REDIRECT_URI + ") is mandatory.")
          .buildWithContextFailing(routingContext)
      );
    }
    UriEnhanced redirectUriEnhanced;
    try {
      redirectUriEnhanced = UriEnhanced.createFromString(redirectUri);
    } catch (IllegalStructure e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The redirect URL (" + redirectUri + ") is not a valid URI.")
          .setCauseException(e)
          .buildWithContextFailing(routingContext)
      );
    }

    clientId = routingContextWrapper.getRequestQueryParameterAsString(AuthQueryProperty.CLIENT_ID.toString());
    if (clientId == null) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setMessage("The redirect client id query property  (" + AuthQueryProperty.CLIENT_ID + ") is mandatory.")
          .buildWithContextFailing(routingContext)
      );
    }

    AuthClient requestingClient = this.apiApp.getAuthClientProvider().getRequestingClient(routingContext);
    if (!clientId.equals(requestingClient.getGuid())) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setMessage("The client id and auth client id are inconsistent.")
          .buildWithContextFailing(routingContext)
      );
    }

    listGuid = routingContextWrapper.getRequestQueryParameterAsString(AuthQueryProperty.LIST_GUID.toString());
    ListGuid listGuidObject;
    try {
      listGuidObject = this.apiApp.getJackson().getDeserializer(ListGuid.class).deserialize(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The listGuid (" + listGuid + ") is not valid")
          .buildWithContextFailing(routingContext)
      );
    }
    Future<ListObject> futureList;
    if (listGuid != null) {
      futureList = this.apiApp.getListProvider().getListByGuidObject(listGuidObject);
    } else {
      futureList = Future.succeededFuture();
    }

    String finalListGuid = listGuid;
    String finalClientId = clientId;
    return futureList
      .compose(listItem -> {

        App requestingApp = this.apiApp.getAppProvider().getRequestingApp(routingContext);
        AppGuid requestingAppGuid = requestingApp.getGuid();
        if (listItem != null) {

          AppGuid listAppGuid = listItem.getApp().getGuid();
          if (!requestingAppGuid.equals(listAppGuid)) {
            return Future.failedFuture(
              TowerFailureException.builder()
                .setMessage("The requesting app (" + requestingAppGuid + ") and the app guid of the list (" + listAppGuid + ") are not consistent. Did you forgot the " + AuthQueryProperty.APP_GUID + " property?")
                .buildWithContextFailing(routingContext)
            );
          }
        }
        if (listItem == null && finalListGuid != null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage("The list (" + finalListGuid + ") was not found")
              .buildWithContextFailing(routingContext)
          );
        }

        JacksonMapperManager jacksonManager = this.apiApp.getJackson();
        /**
         * OAuth is an independent package,
         * we need to set all analytics data
         */
        OAuthState oAuthState = OAuthState
          .createEmpty()
          .setListGuid(finalListGuid)
          .setClientId(finalClientId)
          .setAppGuid(jacksonManager.getSerializer(AppGuid.class).serialize(requestingAppGuid))
          .setAppHandle(jacksonManager.getSerializer(Handle.class).serialize(requestingApp.getHandle()))
          .setRealmIdentifier(jacksonManager.getSerializer(RealmGuid.class).serialize(requestingApp.getRealm().getGuid()))
          .setRealmHandle(jacksonManager.getSerializer(Handle.class).serialize(requestingApp.getRealm().getHandle()))
          .setOrganisationGuid(jacksonManager.getSerializer(OrgaGuid.class).serialize(requestingApp.getRealm().getOrganization().getGuid()))
          .setOrganisationHandle(jacksonManager.getSerializer(Handle.class).serialize(requestingApp.getRealm().getOrganization().getHandle()))
          .setRedirectUri(redirectUriEnhanced);

        return this.apiApp
          .getOauthFlow()
          .step1RedirectToExternalIdentityProvider(
            routingContext,
            provider,
            oAuthState
          )
          .compose(v -> Future.succeededFuture(new ApiResponse<>()));
      });


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
     * Already Signed-in?
     */
    try {
      AuthUser authSignedInUser = this.apiApp.getAuthProvider().getSignedInAuthUser(routingContext);
      if (authSignedInUser != null) {
        routingContext.redirect(redirectUriEnhanced.toString());
        return Future.succeededFuture();
      }
    } catch (NotFoundException e) {
      //
    }

    /**
     * Not signed-in
     */
    AuthClient authClient = this.apiApp.getAuthClientProvider().getRequestingClient(routingContext);
    UriEnhanced url = this.apiApp.getMemberLoginUri(redirectUriEnhanced, authClient);
    routingContext.redirect(url.toString());
    return Future.succeededFuture();

  }

  @Override
  public Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, AuthEmailPost authEmailPost) {

    /**
     * Valid email
     */
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.of(authEmailPost.getUserEmail());
    } catch (EmailCastException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The email (" + authEmailPost.getUserEmail() + ") is not valid")
          .setCauseException(e)
          .build()
      );
    }
    /**
     * Valid redirect Uri
     */
    UriEnhanced redirectUri = ValidationUtil.validateAndGetRedirectUriAsUri(authEmailPost.getRedirectUri());
    Realm requestingRealm = this.apiApp.getRealmProvider().getRequestingRealm(routingContext);
    return this.apiApp.getUserProvider().getUserByEmail(emailAddress, requestingRealm)
      .compose(user -> {
        if (user == null) {
          /**
           * Registration
           */
          return this.apiApp.getUserRegistrationFlow().handleStep1SendEmail(
            routingContext,
            emailAddress,
            requestingRealm,
            redirectUri
          );
        }
        /**
         * Login
         */
        return this.apiApp.getUserEmailLoginFlow()
          .handleStep1SendEmail(routingContext, user, redirectUri);
      });

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

    String realmIdentifier = passwordCredentials.getLoginRealm();
    if (realmIdentifier == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The realm cannot be null", "realmIdentifier", null);
    }

    return this.apiApp
      .getPasswordLoginFlow()
      .login(realmIdentifier, handle, password, routingContext)
      .compose(v -> Future.succeededFuture(new ApiResponse<>()));


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
        .updatePassword(signedInUser.getGuid().getLocalId(), signedInUser.getRealm().getGuid().getLocalId(), passwordOnly.getPassword())
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
  public Future<ApiResponse<Void>> authLogoutGet(RoutingContext routingContext, String clientId, String redirectUri) {

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

    RoutingContextWrapper routingContextWrapper = new RoutingContextWrapper(routingContext);
    clientId = routingContextWrapper.getRequestQueryParameterAsString(AuthQueryProperty.CLIENT_ID.toString());
    String finalClientId = clientId;
    return this.apiApp.getAuthClientProvider().getClientFromClientId(clientId)
      .compose(authClient -> {
        if (authClient == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage("A client could not be found with the client id (" + finalClientId + ")")
              .setMimeToHtml()
              .buildWithContextFailing(routingContext)
          );
        }
        String redirect = this.apiApp
          .getMemberLoginUri(redirectUriEnhanced, authClient)
          .toUrl()
          .toString();
        routingContext.redirect(redirect);

        return Future.succeededFuture(new ApiResponse<>());
      });


  }


  private void utilValidateEmailIdentifierDataUtil(EmailIdentifier emailIdentifier) {
    try {
      EmailAddress.of(emailIdentifier.getUserEmail());
    } catch (EmailCastException e) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The email address is not valid", "userEmail", null);
    }
    String realmIdentifier = emailIdentifier.getRealmIdentifier();
    if (realmIdentifier == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The realm identifier cannot be null.", "realmIdentifier", null);
    }
  }


}
