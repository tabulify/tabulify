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
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthUserClaims;
import net.bytle.vertx.flow.FlowSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytle.tower.eraldy.api.implementer.ListApiImpl.REGISTRATION_EMAIL_SUBJECT_PREFIX;

/**
 * Centralize the request handler for a user registration
 */
public class UserRegistrationFlow {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationFlow.class);

  private static final String USER_GUID_PARAM = ":userGuid";
  private static final String FRONTEND_REGISTER_CONFIRMATION_PATH = "/register/user/confirmation/" + USER_GUID_PARAM;

  /**
   * Handle the registration post
   *
   * @param routingContext  - the routing context
   * @param emailIdentifier - the body post information
   */
  public static Future<ApiResponse<Void>> handleStep1SendEmail(EraldyApiApp apiApp, RoutingContext routingContext, EmailIdentifier emailIdentifier) {

    return apiApp.getRealmProvider()
      .getRealmFromHandle(emailIdentifier.getRealmHandle())
      .onFailure(routingContext::fail)
      .compose(realm -> {

        User newUser = new User();
        newUser.setEmail(emailIdentifier.getUserEmail());
        newUser.setRealm(realm);
        AuthUserClaims authUserClaimsToRegister = UsersUtil.toAuthUser(newUser);

        String realmNameOrHandle = RealmProvider.getNameOrHandle(realm);

        FlowSender realmOwnerSender = UsersUtil.toSenderUser(realm.getOwnerUser());
        JwtClaimsObject jwtClaims = JwtClaimsObject.createFromUser(authUserClaimsToRegister, routingContext);
        String newUserName;
        try {
          newUserName = UsersUtil.getNameOrNameFromEmail(newUser);
        } catch (NotFoundException | AddressException e) {
          return Future.failedFuture(
            VertxRoutingFailureData
              .create()
              .setStatus(HttpStatus.BAD_REQUEST)
              .setDescription("The new user email (" + newUser.getEmail() + ") is not good (" + e.getMessage() + ")")
              .setException(e)
              .failContext(routingContext)
              .getFailedException()
          );
        }
        BMailTransactionalTemplate letter = apiApp
          .getUserRegistrationValidation()
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


        String html = letter.generateHTMLForEmail();
        String text = letter.generatePlainText();

        String mailSubject = "Registration to " + realmNameOrHandle;
        MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(routingContext.vertx());

        MailClient mailClientForListOwner = mailServiceSmtpProvider
          .getVertxMailClientForSenderWithSigning(realmOwnerSender.getEmail());

        String newUserAddressInRfcFormat;
        try {
          newUserAddressInRfcFormat = BMailInternetAddress.of(newUser.getEmail(), newUserName).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            VertxRoutingFailureData
              .create()
              .setStatus(HttpStatus.BAD_REQUEST)
              .setDescription("The new user email (" + newUser.getEmail() + ") is not good (" + e.getMessage() + ")")
              .setException(e)
              .failContext(routingContext)
              .getFailedException()
          );
        }
        String senderEmailInRfc;
        try {
          senderEmailInRfc = BMailInternetAddress.of(realmOwnerSender.getEmail(), realmOwnerSender.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(
            VertxRoutingFailureData
              .create()
              .setStatus(HttpStatus.INTERNAL_ERROR)
              .setDescription("The realm owner email (" + realmOwnerSender.getEmail() + ") is not good (" + e.getMessage() + ")")
              .setException(e)
              .failContext(routingContext)
              .getFailedException()
          );
        }

        MailMessage registrationEmail = mailServiceSmtpProvider
          .createVertxMailMessage()
          .setTo(newUserAddressInRfcFormat)
          .setFrom(senderEmailInRfc)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> VertxRoutingFailureHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + newUser.getEmail() + ") received a registration email for the realm (" + realm.getHandle() + ").";
            MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
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
   * @param ctx             - the context
   * @param jwtClaimsObject - the claims
   */
  public static void handleStep2ClickOnEmailValidationLink(EraldyApiApp apiApp, RoutingContext ctx, JwtClaimsObject jwtClaimsObject) {


    apiApp.getRealmProvider()
      .getRealmFromHandle(jwtClaimsObject.getRealmHandle())
      .onFailure(ctx::fail)
      .onSuccess(realm -> {

        User user = new User();
        user.setRealm(realm);
        user.setEmail(jwtClaimsObject.getEmail());
        UserProvider userProvider = apiApp.getUserProvider();
        userProvider
          .getUserByEmail(user.getEmail(), user.getRealm())
          .onFailure(ctx::fail)
          .onSuccess(userInDb -> {
            if (userInDb != null) {
              // The user was already registered.
              // Possible causes:
              // * The user has clicked two times on the validation link received by email
              // * The user tries to register again
              AuthInternalAuthenticator
                .createWith(apiApp, ctx, userInDb)
                .redirectViaHttp()
                .authenticate();
              return;
            }
            userProvider.insertUser(user)
              .onFailure(ctx::fail)
              .onSuccess(userInserted -> AuthInternalAuthenticator
                .createWith(apiApp, ctx, userInserted)
                .redirectViaFrontEnd(FRONTEND_REGISTER_CONFIRMATION_PATH.replace(USER_GUID_PARAM, userInserted.getGuid()))
                .authenticate());
          });
      });
  }
}
