package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.AuthApiImpl;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndRouter;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.type.UriEnhanced;
import net.bytle.type.time.Date;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static net.bytle.tower.eraldy.api.implementer.ListApiImpl.REGISTRATION_EMAIL_SUBJECT_PREFIX;

/**
 * Utility class to register a user to a list
 */
public class ListRegistrationFlow {

  private static final Logger LOGGER = LogManager.getLogger(ListRegistrationFlow.class);

  private static final String REGISTRATION_GUID_PARAM = ":registrationGuid";

  /**
   * We add the guid in the path to not fall in the
   * list registration operation path (ie /register/list/:registrationGuid)
   * TODO: Add dynamically as a callback the same that it's done for email, See {@link ListRegistrationEmailCallback}
   */
  private static final String FRONTEND_LIST_REGISTRATION_CONFIRMATION_PATH = "/register/list/confirmation/" + REGISTRATION_GUID_PARAM;


  /**
   * Endpoint to register an unauthenticated user to a list
   * It can happen in the OAuth flow and in the Email flow
   *
   * @param ctx              - the request context
   * @param listGuid         - the guid
   * @param user             - the user to register
   * @param optInTime        - the opt-in-Time
   * @param optInIp          - the opt-in-ip
   * @param registrationFlow - the flow used to register the user to the list
   */
  public static void authenticateAndRegisterUserToList(EraldyApiApp apiApp, RoutingContext ctx, String listGuid, User user, Date optInTime, String optInIp, RegistrationFlow registrationFlow) {


    apiApp.getListProvider()
      .getListByGuid(listGuid)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, ctx))
      .compose(list -> {
        Registration inputRegistration = new Registration();
        inputRegistration.setList(list);
        inputRegistration.setSubscriber(user);
        inputRegistration.setOptInTime(optInTime.toIsoString());
        String nowTime = Timestamp.createFromNow().toIsoString();
        inputRegistration.setConfirmationTime(nowTime);
        inputRegistration.setOptInIp(optInIp);
        try {
          String realRemoteClient = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
          inputRegistration.setConfirmationIp(realRemoteClient);
        } catch (NotFoundException e) {
          LOGGER.warn("List registration validation: The remote ip client could not be found. Error: " + e.getMessage());
        }
        inputRegistration.setFlow(registrationFlow);
        return apiApp.getListRegistrationProvider()
          .upsertRegistration(inputRegistration)
          .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, ctx))
          .onSuccess(registration -> {
            addRegistrationConfirmationCookieData(apiApp, ctx, registration);
            AuthInternalAuthenticator
              .createWith(apiApp, ctx, user)
              .redirectViaFrontEnd(getRegistrationConfirmationOperationPath(registration))
              .setMandatoryRedirectUri(false)
              .authenticate();
          });
      });
  }

  public static String getRegistrationOperationPath(RegistrationList registrationList) {
    return "/register/list/" + registrationList.getGuid();
  }

  public static String getRegistrationConfirmationOperationPath(Registration registration) {
    return FRONTEND_LIST_REGISTRATION_CONFIRMATION_PATH.replace(REGISTRATION_GUID_PARAM, registration.getGuid());
  }

  /**
   * Send registration data to the front end via cookie
   *
   * @param routingContext - the context
   * @param registration   - the registration
   */
  public static void addRegistrationConfirmationCookieData(EraldyApiApp apiApp, RoutingContext routingContext, Registration registration) {
    Registration templateClone = apiApp.getListRegistrationProvider()
      .toTemplateClone(registration);
    FrontEndCookie.createCookieData(routingContext, Registration.class)
      .setValue(templateClone);
  }

  /**
   * Handle the post list registration
   *
   * @param routingContext              - the routing context
   * @param publicationSubscriptionPost - the post data
   */
  public static Future<Void> handleStep1SendingValidationEmail(EraldyApiApp apiApp, RoutingContext routingContext, ListRegistrationPostBody publicationSubscriptionPost) {

    /**
     * Email validation
     */
    ValidationUtil.validateEmail(publicationSubscriptionPost.getSubscriberEmail(), "subscriberEmail");

    /**
     * We validate the publication id value
     * (not against the database)
     */
    String publicationGuid = publicationSubscriptionPost.getListGuid();
    if (publicationGuid == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("Publication guid should not be null", "publicationGuid", null);
    }
    Vertx vertx = routingContext.vertx();
    return apiApp.getListProvider()
      .getListByGuid(publicationGuid)
      .compose(registrationList -> {

        User userRegister = new User();
        userRegister.setEmail(publicationSubscriptionPost.getSubscriberEmail());
        Realm listRealm = registrationList.getRealm();
        userRegister.setRealm(listRealm);
        UserClaims userClaimsRegister = UsersUtil.toAuthUser(userRegister);

        JwtClaimsObject jwtClaims = JwtClaimsObject.createFromUser(userClaimsRegister, routingContext)
          .setListGuidClaim(publicationGuid);

        BMailTransactionalTemplate publicationValidationLetter = apiApp
          .getUserListRegistrationCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, userRegister, jwtClaims)
          .setPreview("Validate your registration to the list `" + registrationList.getName() + "`")
          .addIntroParagraph(
            "I just got a subscription request to the list <mark>" + registrationList.getName() + "</mark> with your email." +
              "<br>For bot and consent protections, we check that it was really you asking.")

          .setActionName("Click on this link to validate your registration.")
          .setActionDescription("Click on this link to validate your registration.")
          .addOutroParagraph(
            "Welcome on board. " +
              "<br>Need help, or have any questions? " +
              "<br>Just reply to this email, I ❤ to help."
          );


        String html = publicationValidationLetter.generateHTMLForEmail();
        String text = publicationValidationLetter.generatePlainText();

        String mailSubject = "Registration validation to the list `" + registrationList.getName() + "`";
        MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(vertx);


        User listOwnerUser = ListProvider.getOwnerUser(registrationList);
        String ownerEmailAddressInRfcFormat = UsersUtil.getEmailAddressWithName(listOwnerUser);

        User subscriber = new User();
        subscriber.setEmail(publicationSubscriptionPost.getSubscriberEmail());
        String subscriberAddressWithName = UsersUtil.getEmailAddressWithName(subscriber);

        MailClient mailClientForListOwner = mailServiceSmtpProvider
          .getVertxMailClientForSenderWithSigning(listOwnerUser.getEmail());

        MailMessage registrationEmail = mailServiceSmtpProvider
          .createVertxMailMessage()
          .setTo(subscriberAddressWithName)
          .setFrom(ownerEmailAddressInRfcFormat)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);


        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> VertxRoutingFailureHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + subscriberAddressWithName + ") received a validation email for the list (" + registrationList.getHandle() + ").";
            MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
              .createVertxMailMessage()
              .setTo(ownerEmailAddressInRfcFormat)
              .setFrom(ownerEmailAddressInRfcFormat)
              .setSubject(REGISTRATION_EMAIL_SUBJECT_PREFIX + title)
              .setText(title)
              .setHtml("<html><body>" + title + "</body></html>");
            mailClientForListOwner
              .sendMail(ownerFeedbackEmail)
              .onFailure(t -> LOGGER.error("Error while sending the list owner feedback email", t));

            // Return the response
            return Future.succeededFuture();
          });

      });
  }

  public static void handleStep2EmailValidationLinkClick(EraldyApiApp apiApp, RoutingContext ctx, JwtClaimsObject jwtClaimsObject) {
    String realmHandleClaims = jwtClaimsObject.getRealmHandle();
    String emailClaims = jwtClaimsObject.getEmail();
    String listGuid;
    try {
      listGuid = jwtClaimsObject.getListGuid();
    } catch (NullValueException e) {
      ctx.fail(new InternalException("No guid was in the claims for a user list registration"));
      return;
    }
    Date optInTime = jwtClaimsObject.getIssuedAt();
    String optInIp;
    try {
      optInIp = jwtClaimsObject.getOriginClientIp();
    } catch (NullValueException e) {
      LOGGER.error("The opt-in ip of the Jwt Claims is null");
      optInIp = "";
    }

    String finalOptInIp = optInIp;
    apiApp.getRealmProvider()
      .getRealmFromHandle(realmHandleClaims)
      .onFailure(ctx::fail)
      .onSuccess(realm -> {

        User user = new User();
        user.setRealm(realm);
        user.setEmail(emailClaims);
        UserProvider userProvider = apiApp.getUserProvider();
        userProvider
          .getUserByEmail(user.getEmail(), user.getRealm())
          .onFailure(ctx::fail)
          .onSuccess(userInDb -> {

            Future<User> futureUser;
            if (userInDb != null) {
              futureUser = Future.succeededFuture(userInDb);
            } else {
              futureUser = userProvider.insertUser(user);
            }
            futureUser
              .onFailure(ctx::fail)
              .onSuccess(userToRegister -> authenticateAndRegisterUserToList(apiApp, ctx, listGuid, userToRegister, optInTime, finalOptInIp, RegistrationFlow.EMAIL));

          });
      });


  }

  /**
   * The page that shows the HTML form registration.
   * This step is optional as the form may be hosted
   * elsewhere
   *
   * @param routingContext - the routing context
   * @param listGuid       - the list guid
   * @return the frontend html page
   */
  @SuppressWarnings("unused")
  public static Future<ApiResponse<String>> handleStep0RegistrationForm(EraldyApiApp apiApp, RoutingContext routingContext, String listGuid) {


    Vertx vertx = routingContext.vertx();
    return apiApp.getListProvider()
      .getListByGuid(listGuid)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(list -> {

        if (list == null) {
          VertxRoutingFailureData
            .create()
            .setName("The list was not found")
            .setDescription("The list <mark>" + listGuid + "</mark> was not found.")
            .failContextAsHtml(routingContext);
          return Future.succeededFuture(new ApiResponse<>(HttpStatus.NOT_FOUND));
        }
        Map<String, Object> variables = new HashMap<>();
        list.setGuid(listGuid); // all object does not have any guid by default when retrieved
        User owner = ListProvider.getOwnerUser(list);
        variables.put("list", list);
        variables.put("owner", UsersUtil.getPublicUserForTemplateWithDefaultValues(owner));
        Cookie cookieData = FrontEndCookie.getCookieData(variables);
        routingContext.response().addCookie(cookieData);

        /**
         * Redirect URI is not mandatory
         * The flow may end on the confirmation page.
         */
        UriEnhanced redirectUri = null;
        try {
          redirectUri = AuthApiImpl.getRedirectUri(routingContext);
        } catch (IllegalArgumentException e) {
          return Future.failedFuture(e);
        } catch (NotFoundException e) {
          /**
           * Redirect URI is not mandatory
           * The flow may end on the confirmation page.
           */
        }

        return FrontEndRouter.toPublicNonOAuthPage(apiApp, routingContext, redirectUri);

      });
  }

  /**
   * Shows the confirmation page
   *
   * @param routingContext   - the routing context
   * @param registrationGuid - the registration guid
   * @return Note that the redirection uri is not mandatory
   * and is used by the front end to redirect if present
   */
  public static Future<ApiResponse<String>> handleStep3Confirmation(EraldyApiApp apiApp, RoutingContext routingContext, String registrationGuid) {
    return apiApp.getListRegistrationProvider()
      .getRegistrationByGuid(registrationGuid)
      .onFailure(routingContext::fail)
      .compose(registration -> {
        ListRegistrationFlow.addRegistrationConfirmationCookieData(apiApp, routingContext, registration);
        return FrontEndRouter.toPrivatePage(apiApp, routingContext, false);
      });
  }
}
