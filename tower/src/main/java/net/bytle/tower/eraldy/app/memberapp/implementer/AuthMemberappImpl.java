package net.bytle.tower.eraldy.app.memberapp.implementer;

import io.vertx.core.Future;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.comboapp.ComboAppApp;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.app.memberapp.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.app.memberapp.implementer.flow.UserRegistrationFlow;
import net.bytle.tower.eraldy.app.memberapp.implementer.util.FrontEndRouter;
import net.bytle.tower.eraldy.app.memberapp.openapi.interfaces.AuthMemberapp;
import net.bytle.tower.eraldy.app.memberapp.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.*;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.MailServiceSmtpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class AuthMemberappImpl implements AuthMemberapp {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthMemberappImpl.class);

  public static final String COMBO_CLIENT_ID = ComboAppApp.COMBO_NAME;
  public static final String DATACADAMIA_CLIENT_ID = DatacadamiaDomain.REALM_HANDLE;
  /**
   * We support for now two client id
   */
  public static final List<String> SUPPORTED_CLIENT_IDS = Arrays.asList(
    COMBO_CLIENT_ID,
    DATACADAMIA_CLIENT_ID
  );


  /**
   * Test Url
   * <a href="http://member.nico.lan:8083/login?response_type=code&redirect_uri=yolo&state=youpla&client_id=xxx">...</a>
   */
  @Override
  public Future<ApiResponse<String>> loginOauthAuthorizeGet(RoutingContext routingContext, String responseType, String clientId, String state, String redirectUri) {

    return FrontEndRouter.toPublicOAuthPage(routingContext);

  }


  /**
   * Redirect to the external OAuth authorization end points
   *
   * @param provider       - the OAuth provider (github, ...)
   * @param routingContext - the routing context to write the OAuth state on the session
   * @param listGuid       - the list guid where to register the user (maybe null)
   */
  @Override
  public Future<ApiResponse<Void>> loginOauthProviderGet(RoutingContext routingContext, String provider, String redirectUri, String listGuid, String realmHandle, String realmGuid) {

    /**
     * We don't rely on the argument because they can change of positions on the signature unfortunately
     * or in the openapi definition
     */
    listGuid = routingContext.request().getParam(OAuthQueryProperty.LIST_GUID.toString());

    /**
     * CallBack is mandatory
     * We don't rely on the argument because they can change of positions on the signature unfortunately
     */
    UriEnhanced redirectUriAsUri;
    try {
      redirectUriAsUri = getRedirectUri(routingContext);
      OAuthInternalSession.addRedirectUri(routingContext, redirectUriAsUri);
    } catch (NotFoundException e) {
      /**
       * For a user registration, the redirect uri is mandatory
       * For a list registration, it's not as the flow may end in the confirmation page
       */
      if (listGuid == null) {
        routingContext.fail(HttpStatus.BAD_REQUEST, new IllegalArgumentException("Internal Error: the (" + OAuthInternalSession.REDIRECT_URI_KEY + ") of the client could not be found. Was it passed or set on the login endpoint?"));
        return Future.succeededFuture(new ApiResponse<>(HttpStatus.BAD_REQUEST));
      }
    } catch (IllegalArgumentException e) {
      return Future.failedFuture(e);
    }

    /**
     * Auth Realm is mandatory
     * The `get` request may come from another website
     * and the {@link AuthRealmHandler#getAuthRealmCookie(RoutingContext)} should not be used
     */
    realmGuid = routingContext.request().getParam(OAuthQueryProperty.REALM_GUID.toString());
    realmHandle = routingContext.request().getParam(OAuthQueryProperty.REALM_HANDLE.toString());
    if (realmGuid == null && realmHandle == null) {
      routingContext.fail(HttpStatus.BAD_REQUEST, new IllegalArgumentException("A realm query property (" + OAuthQueryProperty.REALM_HANDLE + " or " + OAuthQueryProperty.REALM_GUID + ") is madnatory."));
      return Future.succeededFuture(new ApiResponse<>(HttpStatus.BAD_REQUEST));
    }


    /**
     * Create the Oauth URL of the provider
     * and redirect to it
     */
    OAuthExternal oAuthExternal;
    try {
      oAuthExternal = OAuthExternal.get(provider);
    } catch (NotFoundException e) {
      return Future.failedFuture(IllegalArgumentExceptions.createWithInputNameAndValue("The OAuth provider (" + provider + ") is unknown", "provider", provider));
    }
    return oAuthExternal
      .getAuthorizeUrl(routingContext, listGuid)
      .compose(url -> {
        routingContext.redirect(url);
        return Future.succeededFuture();
      });

  }

  @Override
  public Future<ApiResponse<String>> loginPasswordGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realm) {
    return FrontEndRouter.toPublicOAuthPage(routingContext);
  }


  @Override
  public Future<ApiResponse<Void>> registerListPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody) {
    return ListRegistrationFlow.handleStep1SendingValidationEmail(routingContext, listRegistrationPostBody)
      .compose(response -> Future.succeededFuture(new ApiResponse<>()));
  }


  @Override
  public Future<ApiResponse<String>> loginEmailGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realm) {
    return FrontEndRouter.toPublicOAuthPage(routingContext);
  }

  @Override
  public Future<ApiResponse<Void>> loginEmailPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {

    validateEmailIdentifierDataUtil(emailIdentifier);

    return UserProvider.createFrom(routingContext.vertx())
      .getUserByEmail(emailIdentifier.getUserEmail(), emailIdentifier.getRealmHandle())
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

        JwtClaimsObject jwtClaims = JwtClaimsObject.createFromUser(userToLogin, routingContext.vertx(), routingContext);
        BMailTransactionalTemplate letter = EraldyMemberApp.get()
          .getUserEmailLoginCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, userToLogin, jwtClaims)
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

        String senderEmailAddressInRfcFormat = UsersUtil.getEmailAddressWithName(userToLogin);

        User sender = new User();
        sender.setEmail("nico@eraldy.com");
        MailClient mailClientForListOwner = mailServiceSmtpProvider
          .getTransactionalMailClientForUser(sender.getEmail());
        String senderEmail = UsersUtil.getEmailAddressWithName(sender);

        MailMessage registrationEmail = mailServiceSmtpProvider
          .createMailMessage()
          .setTo(senderEmailAddressInRfcFormat)
          .setFrom(senderEmail)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> ContextFailureHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + userToLogin.getEmail() + ") received a login email for the realm (" + userToLogin.getRealm().getHandle() + ").";
            MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
              .createMailMessage()
              .setTo(senderEmail)
              .setFrom(senderEmail)
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
  public Future<ApiResponse<String>> loginGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realm) {
    return FrontEndRouter.toPublicOAuthPage(routingContext);
  }


  @Override
  public Future<ApiResponse<OAuthAccessTokenResponse>> loginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri) {

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


    OAuthAccessTokenResponse oAuthAccessTokenResponse = JwtAuthManager
      .get()
      .generateOAuthAccessTokenResponseFromAuthorization(authorization, routingContext);

    return Future.succeededFuture(new ApiResponse<>(oAuthAccessTokenResponse));

  }


  @Override
  public Future<ApiResponse<Void>> loginPasswordPost(RoutingContext routingContext, PasswordCredentials passwordCredentials) {

    String password = passwordCredentials.getLoginPassword();
    if (password == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The password cannot be null", "password", null);
    }
    String handle = passwordCredentials.getLoginHandle();
    if (handle == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The handle cannot be null", "handle", null);
    }
    return RealmProvider.createFrom(routingContext.vertx())
      .getRealmFromHandle(passwordCredentials.getLoginRealm())
      .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
      .compose(realm -> UserProvider.createFrom(routingContext.vertx())
        .getUserByPassword(handle, password, realm)
        .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
        .compose(user -> {
          if (user == null) {
            /**
             * We don't want to leak the information that the user is not found
             */
            return Future.succeededFuture(new ApiResponse<>(HttpStatus.NOT_AUTHORIZED));
          }

          AuthInternalAuthenticator
            .createWith(routingContext, user)
            .redirectViaClient()
            .authenticate();
          return Future.succeededFuture();
        }));

  }

  @Override
  public Future<ApiResponse<String>> loginPasswordResetGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realm) {
    return FrontEndRouter.toPublicOAuthPage(routingContext);
  }

  @Override
  public Future<ApiResponse<Void>> loginPasswordResetPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {

    validateEmailIdentifierDataUtil(emailIdentifier);

    return UserProvider.createFrom(routingContext.vertx())
      .getUserByEmail(emailIdentifier.getUserEmail(), emailIdentifier.getRealmHandle())
      .onFailure(routingContext::fail)
      .compose(userToResetPassword -> {

        /**
         * Email was not found
         * We return a success as we don't want
         * to expose our user
         */
        if (userToResetPassword == null) {
          return Future.succeededFuture();
        }
        String realmNameOrHandle = RealmProvider.getNameOrHandle(userToResetPassword.getRealm());

        JwtClaimsObject jwtClaims = JwtClaimsObject.createFromUser(userToResetPassword, routingContext.vertx(), routingContext);
        BMailTransactionalTemplate letter = EraldyMemberApp.get()
          .getPasswordResetCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, userToResetPassword, jwtClaims)
          .addIntroParagraph(
            "I just got a password reset request on <mark>" + realmNameOrHandle + "</mark> with your email.")
          .setActionName("Click on this link to reset your password.")
          .setActionDescription("Click on this link to to reset your password.")
          .addOutroParagraph(
            "<br>" +
              "<br>Need help, or have any questions? " +
              "<br>Just reply to this email, I ❤ to help."
          );

        String html = letter.generateHTMLForEmail();
        String text = letter.generatePlainText();

        String mailSubject = "Password reset on " + realmNameOrHandle;
        MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(routingContext.vertx());

        String senderEmailAddressInRfcFormat = UsersUtil.getEmailAddressWithName(userToResetPassword);

        User sender = new User();
        sender.setEmail("nico@eraldy.com");
        MailClient mailClientForListOwner = mailServiceSmtpProvider
          .getTransactionalMailClientForUser(sender.getEmail());
        String senderEmail = UsersUtil.getEmailAddressWithName(sender);

        MailMessage registrationEmail = mailServiceSmtpProvider
          .createMailMessage()
          .setTo(senderEmailAddressInRfcFormat)
          .setFrom(senderEmail)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> ContextFailureHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + userToResetPassword.getEmail() + ") received a password reset email for the realm (" + userToResetPassword.getRealm().getHandle() + ").";
            MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
              .createMailMessage()
              .setTo(senderEmail)
              .setFrom(senderEmail)
              .setSubject("Password reset: " + title)
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
  public Future<ApiResponse<String>> loginPasswordUpdateGet(RoutingContext routingContext) {
    return FrontEndRouter.toPrivatePage(routingContext, true);
  }

  @Override
  public Future<ApiResponse<Void>> loginPasswordUpdatePost(RoutingContext routingContext, PasswordOnly passwordOnly) {
    io.vertx.ext.auth.User vertxUser = routingContext.user();
    if (vertxUser == null) {
      return Future.succeededFuture(new ApiResponse<>(HttpStatus.NOT_AUTHORIZED));
    }

    User user = UsersUtil.vertxUserToEraldyUser(vertxUser);

    return UserProvider.createFrom(routingContext.vertx())
      .updatePassword(user, passwordOnly.getPassword())
      .onFailure(err -> routingContext.fail(HttpStatus.INTERNAL_ERROR, err))
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
  public Future<ApiResponse<Void>> logoutGet(RoutingContext context, String redirectUri) {

    /**
     * Context user can come from the JWT
     */
    context.clearUser();
    /**
     * A session may also hold a user
     */
    Session session = context.session();
    if (session != null) {
      session.destroy();
    }

    /**
     * Redirect
     */
    if (redirectUri == null) {
      throw new IllegalArgumentException("A redirect uri or a referer is mandatory on logout");
    }

    EraldyMemberApp eraldyMemberApp = EraldyMemberApp.get();
    String redirect = eraldyMemberApp
      .getLoginUriForEraldyRealm(redirectUri)
      .toUrl()
      .toString();

    context.redirect(redirect);

    return Future.succeededFuture(new ApiResponse<>());

  }

  @Override
  public Future<ApiResponse<String>> registerListConfirmationRegistrationGet(RoutingContext routingContext, String registrationGuid, String redirectUri) {
    /**
     * Note that the redirection uri is not mandatory
     * and is used by the front end to redirect if present
     */
    return ListRegistrationFlow.handleStep3Confirmation(routingContext, registrationGuid);
  }

  @Override
  public Future<ApiResponse<String>> registerUserGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realm) {
    return FrontEndRouter.toPublicOAuthPage(routingContext);
  }


  /**
   * <a href="http://member.nico.lan:8083/combo/member/register/list/lis-yLMzijN">...</a>
   */
  @Override
  public Future<ApiResponse<String>> registerListListGet(RoutingContext routingContext, String listGuid, String redirectUri) {


    return ListRegistrationFlow.handleStep0RegistrationForm(routingContext, listGuid);


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
   * @throws NotFoundException   - if not found
   * @throws IllegalArgumentException - if it's not an URL string
   */
  public static UriEnhanced getRedirectUri(RoutingContext routingContext) throws NotFoundException {
    String redirectUri = routingContext.request().getParam(OAuthQueryProperty.REDIRECT_URI.toString());
    if (redirectUri == null) {
      throw new NotFoundException();
    }
    try {
      return UriEnhanced.createFromString(redirectUri);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri is not a valid URI", OAuthQueryProperty.REDIRECT_URI.toString(), redirectUri);
    }
  }

  @Override
  public Future<ApiResponse<Void>> registerUserPost(RoutingContext routingContext, EmailIdentifier emailIdentifier) {
    validateEmailIdentifierDataUtil(emailIdentifier);
    return UserRegistrationFlow.handleStep1SendEmail(routingContext, emailIdentifier);


  }

  private void validateEmailIdentifierDataUtil(EmailIdentifier emailIdentifier) {
    EmailUtil.validateEmail(emailIdentifier.getUserEmail(), "userEmail");

    String realmHandle = emailIdentifier.getRealmHandle();
    if (realmHandle == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The realm cannot be null.", "realmHandle", null);
    }
  }


}
