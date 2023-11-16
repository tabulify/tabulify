package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AuthApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.OAuthAccessTokenResponse;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import net.bytle.vertx.flow.SmtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthApiImpl implements AuthApi {

  private final EraldyApiApp apiApp;

  public AuthApiImpl(TowerApp apiApp) {
    this.apiApp = (EraldyApiApp) apiApp;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiImpl.class);


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
        VertxFailureHttpException.builder()
          .setStatus(HttpStatusEnum.BAD_REQUEST_400)
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
        VertxFailureHttpException.builder()
          .setStatus(HttpStatusEnum.BAD_REQUEST_400)
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
        VertxFailureHttpException.builder()
          .setStatus(HttpStatusEnum.NOT_AUTHORIZED_401)
          .setMessage("The redirect uri (" + redirectUri + ") is unknown")
          .setMimeToHtml()
          .buildWithContextFailing(routingContext)
      );
    }

    Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(routingContext);

    /**
     * Signed-in
     */
    try {
      User user = this.apiApp.getAuthSignedInUser(routingContext);
      if (user.getRealm().getGuid().equals(authRealm.getGuid())) {
        routingContext.redirect(redirectUriEnhanced.toString());
        return Future.succeededFuture();
      }
    } catch (NotFoundException e) {
      // Not signed in
    }

    /**
     * Not signed-in or realm different
     */
    UriEnhanced url = this.apiApp.getLoginUri(redirectUri, authRealm.getHandle());
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
  public Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {

    utilValidateEmailIdentifierDataUtil(emailIdentifier);

    return apiApp.getUserProvider()
      .getUserByEmail(emailIdentifier.getUserEmail(), emailIdentifier.getRealmIdentifier())
      .onFailure(routingContext::fail)
      .compose(userToLogin -> {

        /**
         * Email was not found
         * We return a success as we don't want
         * to expose our user
         */
        if (userToLogin == null) {
          return Future.succeededFuture();
        }
        String realmNameOrHandle = RealmProvider.getNameOrHandle(userToLogin.getRealm());

        AuthUser jwtClaims = UsersUtil.toAuthUser(userToLogin).addRoutingClaims(routingContext);

        /**
         * Recipient
         */
        String recipientName;
        try {
          recipientName = UsersUtil.getNameOrNameFromEmail(userToLogin);
        } catch (NotFoundException e) {
          throw ValidationException.create("A user name could not be found", "userToRegister", userToLogin.getEmail());
        } catch (AddressException e) {
          throw ValidationException.create("The provided email is not valid", "email", userToLogin.getEmail());
        }
        SmtpSender sender = UsersUtil.toSenderUser(userToLogin.getRealm().getOwnerUser());
        BMailTransactionalTemplate letter = this.apiApp
          .getUserEmailLoginFlow()
          .getCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, sender, recipientName, jwtClaims)
          .addIntroParagraph(
            "I just got a login request to <mark>" + realmNameOrHandle + "</mark> with your email.")
          .setActionName("Click on this link to login automatically.")
          .setActionDescription("Click on this link to login automatically.")
          .addOutroParagraph(
            "<br>" +
              "<br>Need help, or have any questions? " +
              "<br>Just reply to this email, I ❤ to help."
          );

        String html = letter.generateHTMLForEmail();
        String text = letter.generatePlainText();

        String mailSubject = "Login to " + realmNameOrHandle;
        TowerSmtpClient towerSmtpClient = this.apiApp.getApexDomain().getHttpServer().getServer().getSmtpClient();

        String recipientEmailAddressInRfcFormat;
        try {
          recipientEmailAddressInRfcFormat = BMailInternetAddress.of(userToLogin.getEmail(), userToLogin.getGivenName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            VertxFailureHttpException.builder()
              .setStatus(HttpStatusEnum.BAD_REQUEST_400)
              .setMessage("The recipient email (" + userToLogin.getEmail() + ") is not valid")
              .setException(e)
              .buildWithContextFailing(routingContext)
          );
        }
        String senderEmailAddressInRfcFormat;
        try {
          senderEmailAddressInRfcFormat = BMailInternetAddress.of(sender.getEmail(), sender.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            VertxFailureHttpException.builder()
              .setStatus(HttpStatusEnum.INTERNAL_ERROR_500)
              .setMessage("The sender email (" + sender.getEmail() + ") is not valid")
              .setException(e)
              .buildWithContextFailing(routingContext)
          );
        }

        MailClient mailClientForListOwner = towerSmtpClient
          .getVertxMailClientForSenderWithSigning(sender.getEmail());

        MailMessage registrationEmail = towerSmtpClient
          .createVertxMailMessage()
          .setTo(recipientEmailAddressInRfcFormat)
          .setFrom(senderEmailAddressInRfcFormat)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> VertxFailureHttpHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + userToLogin.getEmail() + ") received a login email for the realm (" + userToLogin.getRealm().getHandle() + ").";
            MailMessage ownerFeedbackEmail = towerSmtpClient
              .createVertxMailMessage()
              .setTo(senderEmailAddressInRfcFormat)
              .setFrom(senderEmailAddressInRfcFormat)
              .setSubject("User Login: " + title)
              .setText(text)
              .setHtml(html);
            mailClientForListOwner
              .sendMail(ownerFeedbackEmail)
              .onFailure(t -> LOGGER.error("Error while sending the realm owner feedback email", t));
            return Future.succeededFuture();
          });
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
      .compose(realm -> apiApp.getUserProvider()
        .getUserByPassword(handle, password, realm)
        .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
        .compose(user -> {
          if (user == null) {
            return Future.failedFuture(
              VertxFailureHttpException.builder()
                .setStatus(HttpStatusEnum.NOT_FOUND_404)
                .build()
            );
          }
          AuthUser authUser = UsersUtil.toAuthUser(user);
          new AuthContext(this.apiApp, routingContext, authUser, AuthState.createEmpty())
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

    User user;
    try {
      user = apiApp.getAuthSignedInUser(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        VertxFailureHttpException
          .builder()
          .setStatus(HttpStatusEnum.NOT_LOGGED_IN_401)
          .build()
      );
    }

    return apiApp.getUserProvider()
      .updatePassword(user, passwordOnly.getPassword())
      .compose(futureUser -> {
        /**
         * Because this is a POST, we can't redirect via HTTP
         * The javascript client is doing it.
         */
        return Future.succeededFuture();
      });

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
        VertxFailureHttpException.builder()
          .setStatus(HttpStatusEnum.BAD_REQUEST_400)
          .setMessage("A redirect uri query property (" + AuthQueryProperty.REDIRECT_URI + ") is mandatory in your url in the logout endpoint.")
          .setMimeToHtml()
          .buildWithContextFailing(routingContext)
      );
    }

    Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(routingContext);

    String redirect = this.apiApp
      .getLoginUri(redirectUriEnhanced.toString(), authRealm.getHandle())
      .toUrl()
      .toString();

    routingContext.redirect(redirect);

    return Future.succeededFuture(new ApiResponse<>());

  }

  @Override
  public Future<ApiResponse<Void>> authRegisterListListGuidGet(RoutingContext routingContext, String listGuid) {
    throw new InternalException("Not yet implemented");
  }


  @Override
  public Future<ApiResponse<Void>> authRegisterUserPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {
    utilValidateEmailIdentifierDataUtil(emailIdentifier);
    return this.apiApp.getUserRegistrationFlow().handleStep1SendEmail(routingContext, emailIdentifier);
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
