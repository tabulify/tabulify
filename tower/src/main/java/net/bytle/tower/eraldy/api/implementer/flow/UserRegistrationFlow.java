package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.UserRegisterEmailCallback;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthClientScope;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static net.bytle.tower.eraldy.api.implementer.ListApiImpl.REGISTRATION_EMAIL_SUBJECT_PREFIX;

/**
 * Centralize the request handler for a user registration
 */
public class UserRegistrationFlow extends WebFlowAbs {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationFlow.class);
  private final UserRegisterEmailCallback userCallback;

  public UserRegistrationFlow(TowerApp towerApp) {
    super(towerApp);
    this.userCallback = new UserRegisterEmailCallback(this);
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  @Override
  public FlowType getFlowType() {
    return FlowType.USER_REGISTRATION;
  }

  /**
   * Handle the registration post
   */
  public Future<ApiResponse<Void>> handleStep1SendEmail(RoutingContext routingContext,
                                                        EmailAddress emailAddress,
                                                        Realm realm,
                                                        UriEnhanced redirectUri
  ) {

    /**
     * Check client authorization
     */
    AuthClientScope listRegistration = AuthClientScope.USER_REGISTRATION_FLOW;
    AuthClient authClient = this.getApp().getAuthClientProvider().getRequestingClient(routingContext);
    try {
      this.getApp().getAuthProvider().checkClientAuthorization(authClient, listRegistration);
    } catch (NotAuthorizedException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setMessage("The client don't have any permission to " + listRegistration.getHumanActionName())
        .buildWithContextFailing(routingContext)
      );
    }

    User newUser = new User();
    newUser.setEmailAddress(emailAddress.toNormalizedString());
    newUser.setRealm(realm);
    String realmNameOrHandle = RealmProvider.getNameOrHandle(realm);

    SmtpSender realmOwnerSender = UsersUtil.toSenderUser(realm.getOwnerUser());
    AuthJwtClaims jwtClaims = getApp().getAuthProvider().toJwtClaims(newUser)
      .addRequestClaims(routingContext)
      .setRedirectUri(redirectUri.toUri());

    String newUserName;
    try {
      newUserName = UsersUtil.getNameOrNameFromEmail(newUser);
    } catch (NotFoundException | AddressException e) {
      return Future.failedFuture(
        TowerFailureException
          .builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The new user email (" + newUser.getEmailAddress() + ") is not good (" + e.getMessage() + ")")
          .setCauseException(e)
          .buildWithContextFailing(routingContext)
      );
    }


    /**
     * Add the calling client id
     */
    Map<String, String> clientCallbackQueryProperties = new HashMap<>();
    clientCallbackQueryProperties.put(AuthQueryProperty.CLIENT_ID.toString(), authClient.getGuid());

    BMailTransactionalTemplate letter =
      getApp()
        .getUserRegistrationFlow()
        .getCallback()
        .getCallbackTransactionalEmailTemplateForClaims(routingContext, realmOwnerSender, newUserName, jwtClaims, clientCallbackQueryProperties)
        .setPreview("Validate your registration to `" + realmNameOrHandle + "`")
        .addIntroParagraph(
          "I just got a subscription request to <mark>" + realmNameOrHandle + "</mark> with your email." +
            "<br>For bot and consent protections, we check that it was really you asking.")

        .setActionName("Click on this link to validate your registration.")
        .setActionDescription("Click on this link to validate your registration.")
        .addOutroParagraph(
          "Welcome on board. " +
            "<br>Need help, or have any questions? " +
            "<br>Just reply to this email, I ❤ to help."
        );


    return this.getApp().getHttpServer().getServer().getVertx()
      .executeBlocking(letter::generateHTMLForEmail)
      .compose(html -> {
        String text = letter.generatePlainText();

        String mailSubject = "Registration to " + realmNameOrHandle;
        TowerSmtpClientService towerSmtpClientService = this.getApp().getHttpServer().getServer().getSmtpClient();

        MailClient mailClientForListOwner = towerSmtpClientService
          .getVertxMailClientForSenderWithSigning(realmOwnerSender.getEmail());

        String newUserAddressInRfcFormat;
        try {
          newUserAddressInRfcFormat = BMailInternetAddress.of(newUser.getEmailAddress(), newUserName).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage("The new user email (" + newUser.getEmailAddress() + ") is not good (" + e.getMessage() + ")")
              .setCauseException(e)
              .buildWithContextFailing(routingContext)
          );
        }
        String senderEmailInRfc;
        try {
          senderEmailInRfc = BMailInternetAddress.of(realmOwnerSender.getEmail(), realmOwnerSender.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
              .setMessage("The realm owner email (" + realmOwnerSender.getEmail() + ") is not good (" + e.getMessage() + ")")
              .setCauseException(e)
              .buildWithContextFailing(routingContext)
          );
        }

        MailMessage registrationEmail = towerSmtpClientService
          .createVertxMailMessage()
          .setTo(newUserAddressInRfcFormat)
          .setFrom(senderEmailInRfc)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> TowerFailureHttpHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + newUser.getEmailAddress() + ") received a registration email for the realm (" + realm.getHandle() + ").";
            MailMessage ownerFeedbackEmail = towerSmtpClientService
              .createVertxMailMessage()
              .setTo(senderEmailInRfc)
              .setFrom(senderEmailInRfc)
              .setSubject(REGISTRATION_EMAIL_SUBJECT_PREFIX + title)
              .setText(text)
              .setHtml(html);
            mailClientForListOwner
              .sendMail(ownerFeedbackEmail)
              .onFailure(t -> LOGGER.error("Error while sending the realm owner feedback email", t));
            return Future.succeededFuture();
          });
      });
  }

  /**
   * Second steps:
   *
   * @param ctx       - the context
   * @param jwtClaims - the claims
   */
  public void handleStep2ClickOnEmailValidationLink(RoutingContext ctx, AuthJwtClaims jwtClaims) {

    AuthUser authUser = jwtClaims.toAuthUser();
    String subjectEmail = authUser.getSubjectEmail();
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.of(subjectEmail);
    } catch (EmailCastException e) {
      TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500) // callback our fault
        .setMessage("The AuthUser subject Email (" + subjectEmail + ") is not valid.")
        .buildWithContextFailingTerminal(ctx);
      return;
    }
    AuthProvider authProvider = getApp().getAuthProvider();

    /**
     * The requested realm should be the same as the user realm
     */
    Realm requestingRealm = this.getApp().getRealmProvider().getRequestingRealm(ctx);
    if (!requestingRealm.getGuid().equals(authUser.getRealmGuid())) {
      TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500) // callback our fault
        .setMessage("The requesting realm (" + requestingRealm.getGuid() + ") is not the same as the claims (" + authUser.getRealmGuid() + ")")
        .buildWithContextFailingTerminal(ctx);
      return;
    }
    authProvider
      .getAuthUserForSessionByEmail(emailAddress, requestingRealm)
      .onFailure(ctx::fail)
      .onSuccess(authUserFromGet -> {
        UriEnhanced uriEnhanced = UriEnhanced.createFromUri(jwtClaims.getRedirectUri());
        if (authUserFromGet != null) {
          // The user was already registered.
          // Possible causes:
          // * The user has clicked two times on the validation link received by email
          // Note the user can not register 2 times as if it's the case the user gets a login link
          this.getApp().getAuthNContextManager()
            .newAuthNContext(ctx, this, authUserFromGet, OAuthState.createEmpty(), jwtClaims)
            .redirectViaHttp(uriEnhanced)
            .authenticateSession();
          return;
        }

        /**
         * Analytics
         * (Jwt Claims are used to pass analytics data to the event)
         */
        App requestingApp = this.getApp().getAppProvider().getRequestingApp(ctx);
        jwtClaims.setAppGuid(requestingApp.getGuid());
        jwtClaims.setAppHandle(requestingApp.getHandle());

        /**
         * Insert and login
         */
        authProvider
          .insertUserFromLoginAuthUserClaims(authUser, ctx, this)
          .onFailure(ctx::fail)
          .onSuccess(authUserInserted -> this
            .getApp()
            .getAuthNContextManager()
            .newAuthNContext(ctx, this, authUserInserted, OAuthState.createEmpty(), jwtClaims)
            .redirectViaHttp(uriEnhanced)
            .authenticateSession()
          );
      });

  }


  public UserRegisterEmailCallback getCallback() {
    return this.userCallback;
  }

  /**
   * Handle when a user is authenticated via OAuth
   */
  public Handler<AuthNContext> handleOAuthAuthentication() {
    return authContext -> {

      AuthUser authUser = authContext.getAuthUser();
      if (authUser.getSubject() != null) {
        authContext.next();
        return;
      }

      String realmIdentifier = authContext.getOAuthState().getRealmGuid();
      if (realmIdentifier == null) {
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("For a user registration flow, the realm should have been set in the authentication state")
          .buildWithContextFailingTerminal(authContext.getRoutingContext());
        authContext.next();
        return;
      }

      AuthUser.Builder authUserBuilder = new AuthUser.Builder(authUser.getClaims());
      if (
        this.getApp()
          .getRealmProvider()
          .isRealmGuidIdentifier(realmIdentifier)
      ) {
        authUserBuilder.setRealmGuid(realmIdentifier);
      } else {
        authUserBuilder.setRealmHandle(realmIdentifier);
      }
      AuthUser finalAuthUser = authUserBuilder.build();

      /**
       * Create our principal
       */
      this.getApp()
        .getAuthProvider()
        .getAuthUserForSessionByClaims(finalAuthUser)
        .onFailure(err -> TowerFailureException
          .builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("Error in oauth user get for registration")
          .setCauseException(err)
          .buildWithContextFailingTerminal(authContext.getRoutingContext())
        )
        .onSuccess(authUserForSession -> {
          Future<AuthUser> futureFinalAuthUserForSession;
          if (authUserForSession == null) {
            futureFinalAuthUserForSession = this.getApp()
              .getAuthProvider()
              .insertUserFromLoginAuthUserClaims(finalAuthUser, authContext.getRoutingContext(), this);
          } else {
            futureFinalAuthUserForSession = Future.succeededFuture(authUserForSession);
          }
          futureFinalAuthUserForSession
            .onFailure(err -> TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
              .setMessage("Error in final oauth user registration: " + err.getMessage())
              .setCauseException(err)
              .buildWithContextFailingTerminal(authContext.getRoutingContext())
            )
            .onSuccess(finalAuthUserForSession -> {
              authContext.setAuthUser(finalAuthUserForSession);
              authContext.next();
            });
        });

    };
  }

  /**
   * Add the registration validation callback
   */
  @Override
  public Future<Void> mount() {
    Router router = this.getApp().getHttpServer().getRouter();
    this.userCallback.addCallback(router);
    return super.mount();
  }

}
