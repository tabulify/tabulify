package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.UserRegisterEmailCallback;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.AuthEmailPost;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthState;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.auth.OAuthInternalSession;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytle.tower.eraldy.api.implementer.ListApiImpl.REGISTRATION_EMAIL_SUBJECT_PREFIX;

/**
 * Centralize the request handler for a user registration
 */
public class UserRegistrationFlow extends WebFlowAbs {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationFlow.class);

  private static final String USER_GUID_PARAM = ":userGuid";
  private static final String FRONTEND_REGISTER_CONFIRMATION_PATH = "/register/user/confirmation/" + USER_GUID_PARAM;
  private final UserRegisterEmailCallback userCallback;

  public UserRegistrationFlow(TowerApp towerApp) {
    super(towerApp);
    this.userCallback = new UserRegisterEmailCallback(this);
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  /**
   * Handle the registration post
   *
   * @param routingContext - the routing context
   * @param authEmailPost  - the body post information
   */
  public Future<ApiResponse<Void>> handleStep1SendEmail(RoutingContext routingContext, AuthEmailPost authEmailPost) {

    ValidationUtil.validateEmail(authEmailPost.getUserEmail(), "userEmail");
    String realmIdentifier = authEmailPost.getRealmIdentifier();
    if (realmIdentifier == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The realm identifier cannot be null.", "realmIdentifier", null);
    }

    String redirectUri = authEmailPost.getRedirectUri();
    UriEnhanced redirectUriEnhanced = ValidationUtil.validateAndGetRedirectUriAsUri(redirectUri);
    OAuthInternalSession.addRedirectUri(routingContext, redirectUriEnhanced);

    return getApp()
      .getRealmProvider()
      .getRealmFromIdentifier(authEmailPost.getRealmIdentifier())
      .onFailure(routingContext::fail)
      .compose(realm -> {

        User newUser = new User();
        newUser.setEmail(authEmailPost.getUserEmail());
        newUser.setRealm(realm);


        String realmNameOrHandle = RealmProvider.getNameOrHandle(realm);

        SmtpSender realmOwnerSender = UsersUtil.toSenderUser(realm.getOwnerUser());
        AuthUser jwtClaims = getApp().getAuthProvider().toAuthUser(newUser).addRoutingClaims(routingContext);
        String newUserName;
        try {
          newUserName = UsersUtil.getNameOrNameFromEmail(newUser);
        } catch (NotFoundException | AddressException e) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setStatus(TowerFailureStatusEnum.BAD_REQUEST_400)
              .setMessage("The new user email (" + newUser.getEmail() + ") is not good (" + e.getMessage() + ")")
              .setCauseException(e)
              .buildWithContextFailing(routingContext)
          );
        }
        BMailTransactionalTemplate letter =
          getApp()
            .getUserRegistrationFlow()
            .getCallback()
            .getCallbackTransactionalEmailTemplateForClaims(routingContext, realmOwnerSender, newUserName, jwtClaims)
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


        return this.getApp().getApexDomain().getHttpServer().getServer().getVertx()
          .executeBlocking(letter::generateHTMLForEmail)
          .compose(html -> {
            String text = letter.generatePlainText();

            String mailSubject = "Registration to " + realmNameOrHandle;
            TowerSmtpClient towerSmtpClient = this.getApp().getApexDomain().getHttpServer().getServer().getSmtpClient();

            MailClient mailClientForListOwner = towerSmtpClient
              .getVertxMailClientForSenderWithSigning(realmOwnerSender.getEmail());

            String newUserAddressInRfcFormat;
            try {
              newUserAddressInRfcFormat = BMailInternetAddress.of(newUser.getEmail(), newUserName).toString();
            } catch (AddressException e) {
              return Future.failedFuture(
                TowerFailureException
                  .builder()
                  .setStatus(TowerFailureStatusEnum.BAD_REQUEST_400)
                  .setMessage("The new user email (" + newUser.getEmail() + ") is not good (" + e.getMessage() + ")")
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
                  .setStatus(TowerFailureStatusEnum.INTERNAL_ERROR_500)
                  .setMessage("The realm owner email (" + realmOwnerSender.getEmail() + ") is not good (" + e.getMessage() + ")")
                  .setCauseException(e)
                  .buildWithContextFailing(routingContext)
              );
            }

            MailMessage registrationEmail = towerSmtpClient
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
                String title = "The user (" + newUser.getEmail() + ") received a registration email for the realm (" + realm.getHandle() + ").";
                MailMessage ownerFeedbackEmail = towerSmtpClient
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
      });
  }

  /**
   * Second steps:
   *
   * @param ctx      - the context
   * @param authUserAsClaims - the claims
   */
  public void handleStep2ClickOnEmailValidationLink(RoutingContext ctx, AuthUser authUserAsClaims) {


    AuthProvider authProvider = getApp().getAuthProvider();
    authProvider
      .getAuthUserForSessionByEmailNotNull(authUserAsClaims.getSubjectEmail(), authUserAsClaims.getRealmIdentifier())
      .onFailure(ctx::fail)
      .onSuccess(authUserFromGet -> {
        if (authUserFromGet != null) {
          // The user was already registered.
          // Possible causes:
          // * The user has clicked two times on the validation link received by email
          // * The user tries to register again
          new AuthContext(getApp(), ctx, authUserFromGet, AuthState.createEmpty())
            .redirectViaHttpWithAuthRedirectUriAsParameter(getUriToUserRegistrationConfirmation(authUserFromGet.getSubject()))
            .authenticateSession();
          return;
        }
        authProvider
          .insertUserFromLoginAuthUserClaims(authUserAsClaims, ctx)
          .onFailure(ctx::fail)
          .onSuccess(authUserInserted -> new AuthContext(getApp(), ctx, authUserInserted, AuthState.createEmpty())
            .redirectViaHttpWithAuthRedirectUriAsParameter(getUriToUserRegistrationConfirmation(authUserInserted.getSubject()))
            .authenticateSession()
          );
      });

  }

  private UriEnhanced getUriToUserRegistrationConfirmation(String guid) {
    return this.getApp().getMemberAppUri().setPath(FRONTEND_REGISTER_CONFIRMATION_PATH.replace(USER_GUID_PARAM, guid));
  }

  public UserRegisterEmailCallback getCallback() {
    return this.userCallback;
  }

  /**
   * Handle when a user is authenticated via OAuth
   */
  public Handler<AuthContext> handleOAuthAuthentication() {
    return authContext -> {

      AuthUser authUserClaims = authContext.getAuthUser();
      if (authUserClaims.getSubject() != null) {
        authContext.next();
        return;
      }

      String realmIdentifier = authContext.getAuthState().getRealmIdentifier();
      if (realmIdentifier == null) {
        TowerFailureException.builder()
          .setStatus(TowerFailureStatusEnum.INTERNAL_ERROR_500)
          .setMessage("For a user registration flow, the realm should have been set in the authentication state")
          .buildWithContextFailingTerminal(authContext.getRoutingContext());
        authContext.next();
        return;
      }

      if (
        this.getApp()
          .getRealmProvider()
          .isRealmGuidIdentifier(realmIdentifier)
      ) {
        authUserClaims.setAudience(realmIdentifier);
      } else {
        authUserClaims.setAudienceHandle(realmIdentifier);
      }

      /**
       * Create our principal
       */
      this.getApp()
        .getAuthProvider()
        .getAuthUserForSessionByClaims(authUserClaims)
        .onFailure(err -> TowerFailureException
          .builder()
          .setStatus(TowerFailureStatusEnum.INTERNAL_ERROR_500)
          .setMessage("Error in oauth user get for registration")
          .setCauseException(err)
          .buildWithContextFailingTerminal(authContext.getRoutingContext())
        )
        .onSuccess(authUserForSession -> {
          Future<AuthUser> futureFinalAuthUserForSession;
          if (authUserForSession == null) {
            futureFinalAuthUserForSession = this.getApp()
              .getAuthProvider()
              .insertUserFromLoginAuthUserClaims(authUserClaims, authContext.getRoutingContext());
          } else {
            futureFinalAuthUserForSession = Future.succeededFuture(authUserForSession);
          }
          futureFinalAuthUserForSession
            .onFailure(err -> TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.INTERNAL_ERROR_500)
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
}
