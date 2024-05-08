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
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.type.Handle;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.OAuthInternalSession;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import net.bytle.vertx.jackson.JacksonMapperManager;
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


  public Future<ApiResponse<Void>> handleStep1SendEmail(RoutingContext routingContext, User modelUserToLogin, UriEnhanced redirectUri) {

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

    JacksonMapperManager jackson = this.getApp().getHttpServer().getServer().getJacksonMapperManager();
    AuthJwtClaims jwtClaims = getApp()
      .getAuthProvider()
      .toAuthUserBuilder(modelUserToLogin)
      .setRealmGuid(jackson.getSerializer(RealmGuid.class).serialize(requestingApp.getRealm().getGuid()))
      .setRealmHandle(jackson.getSerializer(Handle.class).serialize(requestingApp.getRealm().getHandle()))
      .setAudienceOwnerUserGuid(jackson.getSerializer(OrgaUserGuid.class).serialize(requestingApp.getRealm().getOwnerUser().getGuid()))
      .setAudienceOwnerOrganizationGuid(jackson.getSerializer(OrgaGuid.class).serialize(requestingApp.getRealm().getOwnerUser().getOrganization().getGuid()))
      .setAudienceOwnerOrganizationHandle(jackson.getSerializer(Handle.class).serialize(requestingApp.getRealm().getOwnerUser().getOrganization().getHandle()))
      .build()
      .toJwtClaims()
      .addRequestClaims(routingContext)
      .setAppGuid(jackson.getSerializer(AppGuid.class).serialize(requestingApp.getGuid()))
      .setAppHandle(jackson.getSerializer(Handle.class).serialize(requestingApp.getHandle()));


    /**
     * Recipient
     */
    String recipientName;
    try {
      recipientName = UsersUtil.getNameOrNameFromEmail(modelUserToLogin);
    } catch (NotFoundException e) {
      throw ValidationException.create("A user name could not be found", "userToRegister", modelUserToLogin.getEmailAddress());
    } catch (AddressException e) {
      throw ValidationException.create("The provided email is not valid", "email", modelUserToLogin.getEmailAddress());
    }
    SmtpSender sender = UsersUtil.toSenderUser(modelUserToLogin.getRealm().getOwnerUser());

    /**
     * Add the calling client id
     */
    Map<String, String> clientCallbackQueryProperties = new HashMap<>();
    String clientIdHash = this.getApp().getJackson().getSerializer(CliGuid.class).serialize(authClient.getGuid());
    clientCallbackQueryProperties.put(AuthQueryProperty.CLIENT_ID.toString(), clientIdHash);

    String realmNameOrHandle = RealmProvider.getNameOrHandle(modelUserToLogin.getRealm());
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
        TowerSmtpClientService towerSmtpClientService = this.getApp().getHttpServer().getServer().getSmtpClient();

        String recipientEmailAddressInRfcFormat;
        try {
          recipientEmailAddressInRfcFormat = BMailInternetAddress.of(modelUserToLogin.getEmailAddress(), modelUserToLogin.getGivenName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .setMessage("The recipient email (" + modelUserToLogin.getEmailAddress() + ") is not valid")
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

        MailClient mailClientForListOwner = towerSmtpClientService
          .getVertxMailClientForSenderWithSigning(sender.getEmail());

        MailMessage registrationEmail = towerSmtpClientService
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
            String title = "The user (" + modelUserToLogin.getEmailAddress() + ") received a login email for the realm (" + modelUserToLogin.getRealm().getHandle() + ").";
            MailMessage ownerFeedbackEmail = towerSmtpClientService
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
