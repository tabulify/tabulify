package net.bytle.tower.eraldy.app.memberapp.implementer.flow;

import io.vertx.core.Future;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.app.memberapp.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.tower.util.JwtClaimsObject;
import net.bytle.vertx.MailServiceSmtpProvider;
import net.bytle.vertx.VertxRoutingFailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytle.tower.eraldy.app.comboprivateapi.implementer.RegistrationComboprivateapiImpl.REGISTRATION_EMAIL_SUBJECT_PREFIX;

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
  public static Future<ApiResponse<Void>> handleStep1SendEmail(RoutingContext routingContext, EmailIdentifier emailIdentifier) {

    return RealmProvider.createFrom(routingContext.vertx())
      .getRealmFromHandle(emailIdentifier.getRealmHandle())
      .onFailure(routingContext::fail)
      .compose(realm -> {

        User userRegister = new User();
        userRegister.setEmail(emailIdentifier.getUserEmail());
        userRegister.setRealm(realm);

        String realmNameOrHandle = RealmProvider.getNameOrHandle(realm);

        JwtClaimsObject jwtClaims = JwtClaimsObject.createFromUser(userRegister, routingContext.vertx(), routingContext);
        BMailTransactionalTemplate letter = EraldyMemberApp.get()
          .getUserRegistrationValidation()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, userRegister, jwtClaims)
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


        String senderEmailAddressInRfcFormat = UsersUtil.getEmailAddressWithName(userRegister);

        User sender = new User();
        sender.setEmail("nico@eraldy.com");
        MailClient mailClientForListOwner = mailServiceSmtpProvider
          .getVertxMailClientForSenderWithSigning(sender.getEmail());
        String senderEmail = UsersUtil.getEmailAddressWithName(sender);

        MailMessage registrationEmail = mailServiceSmtpProvider
          .createVertxMailMessage()
          .setTo(senderEmailAddressInRfcFormat)
          .setFrom(senderEmail)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);

        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> VertxRoutingFailureHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + userRegister.getEmail() + ") received a registration email for the realm (" + realm.getHandle() + ").";
            MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
              .createVertxMailMessage()
              .setTo(senderEmail)
              .setFrom(senderEmail)
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
   * @param ctx - the context
   * @param jwtClaimsObject - the claims
   */
  public static void handleStep2ClickOnEmailValidationLink(RoutingContext ctx, JwtClaimsObject jwtClaimsObject) {


    RealmProvider.createFrom(ctx.vertx())
      .getRealmFromHandle(jwtClaimsObject.getRealmHandle())
      .onFailure(ctx::fail)
      .onSuccess(realm -> {

        User user = new User();
        user.setRealm(realm);
        user.setEmail(jwtClaimsObject.getEmail());
        UserProvider userProvider = UserProvider.createFrom(ctx.vertx());
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
                .createWith(ctx, userInDb)
                .redirectViaHttp()
                .authenticate();
              return;
            }
            userProvider.insertUser(user)
              .onFailure(ctx::fail)
              .onSuccess(userInserted -> AuthInternalAuthenticator
                .createWith(ctx, userInserted)
                .redirectViaFrontEnd(FRONTEND_REGISTER_CONFIRMATION_PATH.replace(USER_GUID_PARAM, userInserted.getGuid()))
                .authenticate());
          });
      });
  }
}
