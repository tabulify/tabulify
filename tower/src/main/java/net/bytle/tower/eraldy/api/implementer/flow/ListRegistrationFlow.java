package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndRouter;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.type.UriEnhanced;
import net.bytle.type.time.Date;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthState;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.auth.OAuthExternalCodeFlow;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static net.bytle.tower.eraldy.api.implementer.ListApiImpl.REGISTRATION_EMAIL_SUBJECT_PREFIX;

/**
 * Utility class to register a user to a list
 */
public class ListRegistrationFlow extends WebFlowAbs {

  private static final Logger LOGGER = LogManager.getLogger(ListRegistrationFlow.class);

  private static final String REGISTRATION_GUID_PARAM = ":registrationGuid";

  /**
   * We add the guid in the path to not fall in the
   * list registration operation path (ie /register/list/:registrationGuid)
   * TODO: Add dynamically as a callback the same that it's done for email, See {@link ListRegistrationEmailCallback}
   */
  private static final String FRONTEND_LIST_REGISTRATION_CONFIRMATION_PATH = "/register/list/confirmation/" + REGISTRATION_GUID_PARAM;
  private final ListRegistrationEmailCallback callback;
  private final FrontEndCookie<Registration> cookieData;

  public ListRegistrationFlow(EraldyApiApp eraldyApiApp) {
    super(eraldyApiApp);
    this.callback = new ListRegistrationEmailCallback(this);
    this.cookieData = FrontEndCookie.createCookieData(Registration.class);
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

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
  public void authenticateAndRegisterUserToList(RoutingContext ctx, String listGuid, User user, Date optInTime, String optInIp, RegistrationFlow registrationFlow) {

    this.getApp()
      .getListProvider()
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
        return this
          .getApp()
          .getListRegistrationProvider()
          .upsertRegistration(inputRegistration)
          .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, ctx))
          .onSuccess(registration -> {
            addRegistrationConfirmationCookieData(ctx, registration);
            new AuthContext(this.getApp(), ctx, UsersUtil.toAuthUserClaims(user), AuthState.createEmpty())
              .redirectViaFrontEnd(getRegistrationConfirmationOperationPath(registration))
              .authenticateSession();
          });
      });
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
  public void addRegistrationConfirmationCookieData(RoutingContext routingContext, Registration registration) {
    Registration templateClone = this.getApp().getListRegistrationProvider()
      .toTemplateClone(registration);
    this.cookieData.setValue(templateClone, routingContext);
  }

  /**
   * Handle the post list registration
   *
   * @param routingContext              - the routing context
   * @param publicationSubscriptionPost - the post data
   */
  public Future<Void> handleStep1SendingValidationEmail(RoutingContext routingContext, ListRegistrationPostBody publicationSubscriptionPost) {

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

    return getApp().getListProvider()
      .getListByGuid(publicationGuid)
      .compose(registrationList -> {

        User subscriber = new User();
        subscriber.setEmail(publicationSubscriptionPost.getSubscriberEmail());
        Realm listRealm = registrationList.getRealm();
        subscriber.setRealm(listRealm);


        AuthUser jwtClaims = UsersUtil
          .toAuthUserClaims(subscriber)
          .addRoutingClaims(routingContext)
          .setListGuidClaim(publicationGuid);

        SmtpSender sender = UsersUtil.toSenderUser(registrationList.getOwnerUser());
        String subscriberRecipientName;
        try {
          subscriberRecipientName = UsersUtil.getNameOrNameFromEmail(subscriber);
        } catch (NotFoundException | AddressException e) {
          return Future.failedFuture(VertxFailureHttp
            .create()
            .setStatus(HttpStatusEnum.BAD_REQUEST_400)
            .setDescription("The name of the subscriber could not be determined (" + e.getMessage() + ")")
            .setException(e)
            .failContext(routingContext)
            .getFailedException()
          );
        }
        BMailTransactionalTemplate publicationValidationLetter = getApp()
          .getUserListRegistrationFlow()
          .getCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, sender, subscriberRecipientName, jwtClaims)
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
        TowerSmtpClient towerSmtpClient = this.getApp().getApexDomain().getHttpServer().getServer().getSmtpClient();


        User listOwnerUser = ListProvider.getOwnerUser(registrationList);
        String ownerEmailAddressInRfcFormat;
        try {
          ownerEmailAddressInRfcFormat = BMailInternetAddress.of(listOwnerUser.getEmail(), listOwnerUser.getName()).toString();
        } catch (AddressException e) {
          return Future.failedFuture(VertxFailureHttp.create().setStatus(HttpStatusEnum.INTERNAL_ERROR_500)
            .setDescription("The list owner email (" + listOwnerUser.getEmail() + ") is not good (" + e.getMessage() + ")")
            .setException(e)
            .failContext(routingContext)
            .getFailedException()
          );
        }

        String subscriberAddressWithName;
        try {
          subscriberAddressWithName = BMailInternetAddress.of(subscriber.getEmail(), subscriberRecipientName).toString();
        } catch (AddressException e) {
          return Future.failedFuture(VertxFailureHttp
            .create()
            .setStatus(HttpStatusEnum.BAD_REQUEST_400)
            .setDescription("The subscriber email (" + subscriber.getEmail() + ") is not good (" + e.getMessage() + ")")
            .setException(e)
            .failContext(routingContext)
            .getFailedException()
          );
        }

        MailClient mailClientForListOwner = towerSmtpClient
          .getVertxMailClientForSenderWithSigning(listOwnerUser.getEmail());

        MailMessage registrationEmail = towerSmtpClient
          .createVertxMailMessage()
          .setTo(subscriberAddressWithName)
          .setFrom(ownerEmailAddressInRfcFormat)
          .setSubject(mailSubject)
          .setText(text)
          .setHtml(html);


        return mailClientForListOwner
          .sendMail(registrationEmail)
          .onFailure(t -> VertxFailureHttpHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
          .compose(mailResult -> {

            // Send feedback to the list owner
            String title = "The user (" + subscriberAddressWithName + ") received a validation email for the list (" + registrationList.getHandle() + ").";
            MailMessage ownerFeedbackEmail = towerSmtpClient
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

  public void handleStep2EmailValidationLinkClick(RoutingContext ctx, AuthUser authUser) {
    String realmHandleClaims = authUser.getRealmIdentifier();
    String emailClaims = authUser.getSubjectEmail();
    String listGuid;
    try {
      listGuid = authUser.getListGuid();
    } catch (NullValueException e) {
      ctx.fail(new InternalException("No guid was in the claims for a user list registration"));
      return;
    }
    Date optInTime = authUser.getIssuedAt();
    String optInIp;
    try {
      optInIp = authUser.getOriginClientIp();
    } catch (NullValueException e) {
      LOGGER.error("The opt-in ip of the Jwt Claims is null");
      optInIp = "";
    }

    String finalOptInIp = optInIp;
    this.getApp().getRealmProvider()
      .getRealmFromHandle(realmHandleClaims)
      .onFailure(ctx::fail)
      .onSuccess(realm -> {

        User user = new User();
        user.setRealm(realm);
        user.setEmail(emailClaims);
        UserProvider userProvider = this.getApp().getUserProvider();
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
              .onSuccess(userToRegister -> authenticateAndRegisterUserToList(ctx, listGuid, userToRegister, optInTime, finalOptInIp, RegistrationFlow.EMAIL));

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
          VertxFailureHttp
            .create()
            .setName("The list was not found")
            .setDescription("The list <mark>" + listGuid + "</mark> was not found.")
            .failContextAsHtml(routingContext);
          return Future.succeededFuture(new ApiResponse<>(HttpStatusEnum.NOT_FOUND_404.getStatusCode()));
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
          redirectUri = OAuthExternalCodeFlow.getRedirectUri(routingContext);
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
  public Future<ApiResponse<String>> handleStep3Confirmation(RoutingContext routingContext, String registrationGuid) {
    return this.getApp().getListRegistrationProvider()
      .getRegistrationByGuid(registrationGuid)
      .onFailure(routingContext::fail)
      .compose(registration -> {
        addRegistrationConfirmationCookieData(routingContext, registration);
        return FrontEndRouter.toPrivatePage(this.getApp(), routingContext, false);
      });
  }

  public ListRegistrationEmailCallback getCallback() {
    return this.callback;
  }

  /**
   * Handle when a user is authenticated via OAuth
   * If the user authenticate via a list, we register
   * the user to the list
   */
  public Handler<AuthContext> handleStepOAuthAuthentication() {
    return authContext -> {

      String listGuid = authContext.getAuthState().getListGuid();
      if (listGuid == null) {
        authContext.next();
        return;
      }

      AuthUser authUser = authContext.getAuthUser();
      RoutingContext ctx = authContext.getRoutingContext();

      /**
       * A list registration
       */
      Date optInTime = Date.createFromNow();
      String optInIp;
      try {

        optInIp = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
      } catch (NotFoundException e) {
        LOGGER.warn("Oauth List registration: The remote ip client could not be found. Error: " + e.getMessage());
        optInIp = "";
      }

      User user = UsersUtil.toEraldyUser(authUser, this.getApp());
      this.authenticateAndRegisterUserToList(ctx, listGuid, user, optInTime, optInIp, RegistrationFlow.OAUTH);
      authContext.next();

    };
  }

}
