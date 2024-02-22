package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.UserLoginEmailCallback;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthClientScope;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.auth.OAuthInternalSession;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EmailLoginFlow extends WebFlowAbs {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailLoginFlow.class);
  private final UserLoginEmailCallback step2Callback;

  public EmailLoginFlow(TowerApp towerApp) {
    super(towerApp);
    this.step2Callback = new UserLoginEmailCallback(this);
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  @Override
  public FlowType getFlowType() {
    return FlowType.EMAIL_LOGIN;
  }

  public UserLoginEmailCallback getStep2Callback() {
    return this.step2Callback;
  }


  public Future<ApiResponse<Void>> handleStep1SendEmail(RoutingContext routingContext, User userToLogin, UriEnhanced redirectUri) {

    AuthClient authClient = this.getApp().getAuthClientProvider().getRequestingClient(routingContext);
    AuthClientScope loginEmailFlow = AuthClientScope.LOGIN_EMAIL_FLOW;
    try {

      this.getApp().getAuthProvider().checkClientAuthorization(authClient, loginEmailFlow);
    } catch (NotAuthorizedException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
        .setMessage("You don't have any permission to " + loginEmailFlow.getHumanActionName())
        .buildWithContextFailing(routingContext)
      );
    }

    /**
     * By making the redirect URI part of the session,
     * the link can't be used 2 times
     */
    OAuthInternalSession.addRedirectUri(routingContext, redirectUri);

    App requestingApp = this.getApp().getAppProvider().getRequestingApp(routingContext);

    AuthUser jwtClaims = getApp()
      .getAuthProvider()
      .toJwtClaims(userToLogin)
      .addRequestClaims(routingContext)
      .setAppGuid(requestingApp.getGuid())
      .setAppHandle(requestingApp.getHandle())
      .setRealmGuid(requestingApp.getRealm().getGuid())
      .setRealmHandle(requestingApp.getRealm().getHandle())
      .setOrganizationGuid(requestingApp.getRealm().getOrganization().getGuid())
      .setOrganizationHandle(requestingApp.getRealm().getOrganization().getHandle());


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

    /**
     * Add the calling client id
     */
    Map<String, String> clientCallbackQueryProperties = new HashMap<>();
    clientCallbackQueryProperties.put(AuthQueryProperty.CLIENT_ID.toString(), authClient.getGuid());

    String realmNameOrHandle = RealmProvider.getNameOrHandle(userToLogin.getRealm());
    BMailTransactionalTemplate letter = getApp()
      .getUserEmailLoginFlow()
      .getStep2Callback()
      .getCallbackTransactionalEmailTemplateForClaims(routingContext, sender, recipientName, jwtClaims, clientCallbackQueryProperties)
      .addIntroParagraph(
        "I just got a login request to <mark>" + realmNameOrHandle + "</mark> with your email.")
      .setActionName("Click on this link to login automatically.")
      .setActionDescription("Click on this link to login automatically.")
      .addOutroParagraph(
        "<br>" +
          "<br>Need help, or have any questions? " +
          "<br>Just reply to this email, I ❤ to help."
      );

    return this.getApp().getHttpServer().getServer().getVertx()
      .executeBlocking(letter::generateHTMLForEmail)
      .compose(html -> {

        String text = letter.generatePlainText();

        String mailSubject = "Login to " + realmNameOrHandle;
        TowerSmtpClient towerSmtpClient = this.getApp().getHttpServer().getServer().getSmtpClient();

        String recipientEmailAddressInRfcFormat;
        try {
          recipientEmailAddressInRfcFormat = BMailInternetAddress.of(userToLogin.getEmail(), userToLogin.getGivenName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage("The recipient email (" + userToLogin.getEmail() + ") is not valid")
              .setCauseException(e)
              .buildWithContextFailing(routingContext)
          );
        }
        String senderEmailAddressInRfcFormat;
        try {
          senderEmailAddressInRfcFormat = BMailInternetAddress.of(sender.getEmail(), sender.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
              .setMessage("The sender email (" + sender.getEmail() + ") is not valid")
              .setCauseException(e)
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
          .onFailure(t -> TowerFailureHttpHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
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

  /**
   * Add the email login validation callback
   */
  @Override
  public Future<Void> mount() {
    Router router = this.getApp().getHttpServer().getRouter();
    this.step2Callback.addCallback(router);
    return super.mount();
  }
}
