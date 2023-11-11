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
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AuthApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.PasswordCredentials;
import net.bytle.tower.eraldy.model.openapi.PasswordOnly;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.util.DatacadamiaDomain;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import net.bytle.vertx.flow.SmtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class AuthApiImpl implements AuthApi {

  private final EraldyApiApp apiApp;

  public AuthApiImpl(TowerApp apiApp) {
    this.apiApp = (EraldyApiApp) apiApp;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiImpl.class);


  public static final String DATACADAMIA_CLIENT_ID = DatacadamiaDomain.REALM_HANDLE;
  /**
   * We support for now two client id
   */
  public static final List<String> SUPPORTED_CLIENT_IDS = Arrays.asList(
    "combo",
    DATACADAMIA_CLIENT_ID
  );


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
     * CallBack is mandatory
     * We don't rely on the argument because they can change of positions on the signature unfortunately
     */
    UriEnhanced redirectUriAsUri;
    try {
      redirectUriAsUri = utilGetRedirectUri(routingContext);
      OAuthInternalSession.addRedirectUri(routingContext, redirectUriAsUri);
    } catch (NotFoundException e) {
      /**
       * For a user registration, the redirect uri is mandatory
       * For a list registration, it's not as the flow may end in the confirmation page
       */
      if (listGuid == null) {
        routingContext.fail(HttpStatus.BAD_REQUEST.httpStatusCode(), new IllegalArgumentException("Internal Error: the (" + OAuthInternalSession.REDIRECT_URI_KEY + ") of the client could not be found. Was it passed or set on the login endpoint?"));
        return Future.succeededFuture(new ApiResponse<>(HttpStatus.BAD_REQUEST.httpStatusCode()));
      }
    } catch (IllegalArgumentException e) {
      return Future.failedFuture(e);
    }

    /**
     * Auth Realm is mandatory
     * The `get` request may come from another website
     * and the {@link AuthRealmHandler#getAuthRealmCookie(RoutingContext)} should not be used
     */
    realmIdentifier = routingContext.request().getParam(AuthQueryProperty.REALM_IDENTIFIER.toString());
    if (realmIdentifier == null) {
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatus.BAD_REQUEST)
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
      oAuthExternalProvider = this.apiApp.getOAuthExternal().getProvider(provider);
    } catch (NotFoundException e) {
      return Future.failedFuture(IllegalArgumentExceptions.createWithInputNameAndValue("The OAuth provider (" + provider + ") is unknown", "provider", provider));
    }
    String url = oAuthExternalProvider
      .getAuthorizeUrl(routingContext, listGuid);
    routingContext.redirect(url);

    return Future.succeededFuture();


  }


  @Override
  public Future<ApiResponse<Void>> authLoginAuthorizeGet(RoutingContext routingContext, String redirectUri) {

    /**
     * Redirect
     */
    UriEnhanced redirectUriEnhanced;
    try {
      redirectUriEnhanced = utilGetRedirectUri(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatus.BAD_REQUEST)
          .setDescription("A redirect uri query property (" + AuthQueryProperty.REDIRECT_URI + ") is mandatory in your url in the authorize endpoint")
          .failContext(routingContext)
          .setMimeToHtml()
          .getFailedException()
      );
    }

    /**
     * Realm is only eraldy for now
     */
    String realmIdentifier;
    try {
      realmIdentifier = this.utilGetRealmHandleFromRedirectUri(redirectUriEnhanced);
    } catch (NotAuthorizedException e) {
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatus.NOT_AUTHORIZED)
          .setDescription("The redirect uri (" + redirectUri + ") is unknown")
          .failContext(routingContext)
          .setMimeToHtml()
          .getFailedException()
      );
    }

    try {
      this.apiApp.getAuthSignedInUser(routingContext);
      routingContext.redirect(redirectUriEnhanced.toString());
      return Future.succeededFuture();
    } catch (NotFoundException e) {
      UriEnhanced url = this.apiApp.getLoginUri(redirectUri, realmIdentifier);
      routingContext.redirect(url.toString());
      return Future.succeededFuture();
    }


  }

  private String utilGetRealmHandleFromRedirectUri(UriEnhanced redirectUriEnhanced) throws NotAuthorizedException {
    TowerApexDomain eraldyApexDomain = this.apiApp.getApexDomain();
    if (!(redirectUriEnhanced.getApexWithoutPort().equals("localhost") || redirectUriEnhanced.getApexWithoutPort().equals(eraldyApexDomain.getApexNameWithoutPort()))) {
      throw new NotAuthorizedException();
    }
    return eraldyApexDomain.getRealmHandle();
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

        AuthUser jwtClaims = UsersUtil.toAuthUserClaims(userToLogin).addRoutingClaims(routingContext);

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
        MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(routingContext.vertx());

        String recipientEmailAddressInRfcFormat;
        try {
          recipientEmailAddressInRfcFormat = BMailInternetAddress.of(userToLogin.getEmail(), userToLogin.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            VertxRoutingFailureData.create()
              .setStatus(HttpStatus.BAD_REQUEST)
              .setDescription("The recipient email (" + userToLogin.getEmail() + ") is not valid")
              .setException(e)
              .failContext(routingContext)
              .getFailedException()
          );
        }
        String senderEmailAddressInRfcFormat;
        try {
          senderEmailAddressInRfcFormat = BMailInternetAddress.of(sender.getEmail(), sender.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            VertxRoutingFailureData.create()
              .setStatus(HttpStatus.INTERNAL_ERROR)
              .setDescription("The sender email (" + sender.getEmail() + ") is not valid")
              .setException(e)
              .failContext(routingContext)
              .getFailedException()
          );
        }

        MailClient mailClientForListOwner = mailServiceSmtpProvider
          .getVertxMailClientForSenderWithSigning(sender.getEmail());

        MailMessage registrationEmail = mailServiceSmtpProvider
          .createVertxMailMessage()
          .setTo(recipientEmailAddressInRfcFormat)
          .setFrom(senderEmailAddressInRfcFormat)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> VertxRoutingFailureHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + userToLogin.getEmail() + ") received a login email for the realm (" + userToLogin.getRealm().getHandle() + ").";
            MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
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
      .getRealmFromHandle(passwordCredentials.getLoginRealm())
      .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
      .compose(realm -> apiApp.getUserProvider()
        .getUserByPassword(handle, password, realm)
        .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
        .compose(user -> {
          if (user == null) {
            return Future.failedFuture(
              VertxRoutingFailureData.create()
                .setStatus(HttpStatus.NOT_FOUND)
                .getFailedException()
            );
          }
          AuthUser authUser = UsersUtil.toAuthUserClaims(user);
          AuthInternalAuthenticator
            .createWith(apiApp, routingContext, authUser)
            .redirectViaClient()
            .authenticate();
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
        VertxRoutingFailureData
          .create()
          .setStatus(HttpStatus.NOT_LOGGED_IN)
          .getFailedException()
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
      redirectUriEnhanced = utilGetRedirectUri(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatus.BAD_REQUEST)
          .setDescription("A redirect uri query property (" + AuthQueryProperty.REDIRECT_URI + ") is mandatory in your url in the logout endpoint.")
          .failContext(routingContext)
          .setMimeToHtml()
          .getFailedException()
      );
    }

    String realmIdentifier;
    try {
      realmIdentifier = this.utilGetRealmHandleFromRedirectUri(redirectUriEnhanced);
    } catch (NotAuthorizedException e) {
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatus.NOT_AUTHORIZED)
          .setDescription("The redirect uri (" + redirectUri + ") is unknown")
          .failContext(routingContext)
          .setMimeToHtml()
          .getFailedException()
      );
    }

    String redirect = this.apiApp
      .getLoginUri(redirectUriEnhanced.toString(), realmIdentifier)
      .toUrl()
      .toString();

    routingContext.redirect(redirect);

    return Future.succeededFuture(new ApiResponse<>());

  }


  /**
   * Because of a bug on the order in parameters signature, we get the value
   * from the context
   * <p>
   * We take the value of the parameters from the context
   * because if there is a switch on position in the open api specification,
   * the data are still string and the api does not cry even if the data received
   * is not the good one
   *
   * @param routingContext - the routing context
   * @return the redirect Uri
   * @throws NotFoundException        - if not found
   * @throws IllegalArgumentException - if it's not an URL string
   */
  public static UriEnhanced utilGetRedirectUri(RoutingContext routingContext) throws NotFoundException {
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

  @Override
  public Future<ApiResponse<Void>> authUserRegisterPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {
    utilValidateEmailIdentifierDataUtil(emailIdentifier);
    return this.apiApp.getUserRegistrationFlow().handleStep1SendEmail(routingContext, emailIdentifier);
  }


  private void utilValidateEmailIdentifierDataUtil(EmailIdentifier emailIdentifier) {
    ValidationUtil.validateEmail(emailIdentifier.getUserEmail(), "userEmail");
    String realmIdentifier = emailIdentifier.getRealmIdentifier();
    if (realmIdentifier == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The realm identifier cannot be null.", "realmIdentifier", null);
    }
  }


}
