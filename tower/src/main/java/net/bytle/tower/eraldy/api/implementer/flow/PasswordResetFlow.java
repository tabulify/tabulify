package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.PasswordResetEmailCallback;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import net.bytle.vertx.flow.WebFlowEmailCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PasswordResetFlow extends WebFlowAbs {

  static Logger LOGGER = LogManager.getLogger(PasswordResetFlow.class);

  private final PasswordResetEmailCallback step2Callback;

  public PasswordResetFlow(TowerApp towerApp) {
    super(towerApp);
    this.step2Callback = new PasswordResetEmailCallback(this);
  }

  public WebFlowEmailCallback getPasswordResetCallback() {
    return this.step2Callback;
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  public Future<ApiResponse<Void>> step1SendEmail(RoutingContext routingContext, EmailIdentifier emailIdentifier) {
    return getApp()
      .getUserProvider()
      .getUserByEmail(emailIdentifier.getUserEmail(), emailIdentifier.getRealmIdentifier())
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

        SmtpSender sender = UsersUtil.toSenderUser(userToResetPassword.getRealm().getOwnerUser());
        String recipientName;
        try {
          recipientName = UsersUtil.getNameOrNameFromEmail(userToResetPassword);
        } catch (NotFoundException | AddressException e) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
            .setMessage("A name for the user to reset could not be found (" + e.getMessage() + ")")
            .setCauseException(e)
            .buildWithContextFailing(routingContext)
          );
        }
        AuthUser jwtClaims = getApp().getAuthProvider().toAuthUser(userToResetPassword).addRoutingClaims(routingContext);

        BMailTransactionalTemplate letter = this.step2Callback
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, sender, recipientName, jwtClaims)
          .addIntroParagraph(
            "I just got a password reset request on <mark>" + realmNameOrHandle + "</mark> with your email.")
          .setActionName("Click on this link to reset your password.")
          .setActionDescription("Click on this link to to reset your password.")
          .addOutroParagraph(
            "<br>" +
              "<br>Need help, or have any questions? " +
              "<br>Just reply to this email, I ❤ to help."
          );

        return this.getApp().getApexDomain().getHttpServer().getServer().getVertx()
          .executeBlocking(letter::generateHTMLForEmail)
          .compose(html -> {
            String text = letter.generatePlainText();

            String mailSubject = "Password reset on " + realmNameOrHandle;
            TowerSmtpClient towerSmtpClient = this.getApp().getApexDomain().getHttpServer().getServer().getSmtpClient();

            String recipientEmailAddressInRfcFormat;
            try {
              recipientEmailAddressInRfcFormat = BMailInternetAddress.of(userToResetPassword.getEmail(), recipientName).toString();
            } catch (AddressException e) {
              return Future.failedFuture(TowerFailureException.builder()
                .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
                .setMessage("The email for the user to reset (" + userToResetPassword.getEmail() + ") is not valid (" + e.getMessage() + ")")
                .setCauseException(e)
                .buildWithContextFailing(routingContext)
              );
            }
            String senderEmail;
            try {
              senderEmail = BMailInternetAddress.of(sender.getEmail(), sender.getName()).toString();
            } catch (AddressException e) {
              return Future.failedFuture(TowerFailureException.builder()
                .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
                .setMessage("The sender email (" + sender.getEmail() + ") is not valid (" + e.getMessage() + ")")
                .setCauseException(e)
                .buildWithContextFailing(routingContext)
              );
            }

            MailClient mailClientForListOwner = towerSmtpClient
              .getVertxMailClientForSenderWithSigning(sender.getEmail());

            MailMessage registrationEmail = towerSmtpClient
              .createVertxMailMessage()
              .setTo(recipientEmailAddressInRfcFormat)
              .setFrom(senderEmail)
              .setSubject(mailSubject)
              .setText(text)
              .setHtml(html);

            return mailClientForListOwner
              .sendMail(registrationEmail)
              .onFailure(t -> TowerFailureHttpHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
              .compose(mailResult -> {

                // Send feedback to the list owner
                String title = "The user (" + userToResetPassword.getEmail() + ") received a password reset email for the realm (" + userToResetPassword.getRealm().getHandle() + ").";
                MailMessage ownerFeedbackEmail = towerSmtpClient
                  .createVertxMailMessage()
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
      });
  }
}
