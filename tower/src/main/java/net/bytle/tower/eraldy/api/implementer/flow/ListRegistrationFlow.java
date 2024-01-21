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
import net.bytle.exception.*;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndCookie;
import net.bytle.tower.eraldy.api.implementer.util.FrontEndRouter;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.util.Guid;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthState;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.auth.OAuthExternalCodeFlow;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import net.bytle.vertx.flow.WebFlowType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
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
  private final FrontEndCookie<String> cookieData;

  public ListRegistrationFlow(EraldyApiApp eraldyApiApp) {
    super(eraldyApiApp);
    this.callback = new ListRegistrationEmailCallback(this);
    this.cookieData = FrontEndCookie.createCookieData(String.class);
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  @Override
  public WebFlowType getFlowType() {
    return WebFlowType.LIST_REGISTRATION;
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
  public Future<ListUser> registerUserToList(RoutingContext ctx, Guid listGuid, User user, LocalDateTime optInTime, String optInIp, ListUserSource registrationFlow) {

    return this.getApp()
      .getListProvider()
      .getListByGuidObject(listGuid)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, ctx))
      .compose(list -> {
        ListUser inputListUser = new ListUser();
        inputListUser.setList(list);
        inputListUser.setUser(user);
        inputListUser.setInOptInTime(optInTime);
        inputListUser.setInOptInConfirmationTime(LocalDateTime.now());
        inputListUser.setInOptInIp(optInIp);
        try {
          String realRemoteClient = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
          inputListUser.setInOptInConfirmationIp(realRemoteClient);
        } catch (NotFoundException e) {
          LOGGER.warn("List registration validation: The remote ip client could not be found. Error: " + e.getMessage());
        }
        inputListUser.setInSourceId(registrationFlow);
        return this
          .getApp()
          .getListRegistrationProvider()
          .upsertListUser(inputListUser)
          .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, ctx))
          .compose(Future::succeededFuture);
      });
  }

  public UriEnhanced getRegistrationConfirmationOperationPath(ListUser listUser) {
    String registrationConfirmationOperationPath = FRONTEND_LIST_REGISTRATION_CONFIRMATION_PATH.replace(REGISTRATION_GUID_PARAM, listUser.getGuid());
    return this.getApp().getMemberAppUri().setPath(registrationConfirmationOperationPath);
  }

  /**
   * Send registration data to the front end via cookie
   *
   * @param routingContext - the context
   * @param listUser   - the registration
   */
  public void addRegistrationConfirmationCookieData(RoutingContext routingContext, ListUser listUser) {


      String listUserAsJson = this.getApp().getListRegistrationProvider().toTemplateJson(listUser);

      this.cookieData.setValue(listUserAsJson, routingContext);
  }

  /**
   * Handle the post list registration
   *
   * @param routingContext              - the routing context
   * @param listUserPostBody - the post data
   */
  public Future<Void> handleStep1SendingValidationEmail(RoutingContext routingContext, ListUserPostBody listUserPostBody) {

    /**
     * Email validation
     */
    ValidationUtil.validateEmail(listUserPostBody.getUserEmail(), "userEmail");

    /**
     * We validate the publication id value
     * (not against the database)
     */
    String listGuidHash = listUserPostBody.getListGuid();
    if (listGuidHash == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("List guid should not be null", "listGuid", null);
    }

    return getApp().getListProvider()
      .getListByGuidHashIdentifier(listGuidHash)
      .compose(registrationList -> {

        User user = new User();
        user.setEmail(listUserPostBody.getUserEmail());
        Realm listRealm = registrationList.getRealm();
        user.setRealm(listRealm);


        AuthUser jwtClaims = getApp().getAuthProvider()
          .toAuthUser(user)
          .addRoutingClaims(routingContext)
          .setListGuidClaim(listGuidHash);

        SmtpSender sender = UsersUtil.toSenderUser(registrationList.getOwnerUser());
        String subscriberRecipientName;
        try {
          subscriberRecipientName = UsersUtil.getNameOrNameFromEmail(user);
        } catch (NotFoundException | AddressException e) {
          return Future.failedFuture(TowerFailureException
            .builder()
            .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
            .setMessage("The name of the user could not be determined (" + e.getMessage() + ")")
            .setCauseException(e)
            .buildWithContextFailing(routingContext)
          );
        }
        BMailTransactionalTemplate letter = getApp()
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

        return this.getApp().getApexDomain().getHttpServer().getServer().getVertx()
          .executeBlocking(letter::generateHTMLForEmail)
          .compose(html -> {
            String text = letter.generatePlainText();

            String mailSubject = "Registration validation to the list `" + registrationList.getName() + "`";
            TowerSmtpClient towerSmtpClient = this.getApp().getApexDomain().getHttpServer().getServer().getSmtpClient();


            User listOwnerUser = ListProvider.getOwnerUser(registrationList);
            String ownerEmailAddressInRfcFormat;
            try {
              ownerEmailAddressInRfcFormat = BMailInternetAddress.of(listOwnerUser.getEmail(), listOwnerUser.getGivenName()).toString();
            } catch (AddressException e) {
              return Future.failedFuture(TowerFailureException.builder().setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
                .setMessage("The list owner email (" + listOwnerUser.getEmail() + ") is not good (" + e.getMessage() + ")")
                .setCauseException(e)
                .buildWithContextFailing(routingContext)
              );
            }

            String subscriberAddressWithName;
            try {
              subscriberAddressWithName = BMailInternetAddress.of(user.getEmail(), subscriberRecipientName).toString();
            } catch (AddressException e) {
              return Future.failedFuture(TowerFailureException
                .builder()
                .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
                .setMessage("The subscriber email (" + user.getEmail() + ") is not good (" + e.getMessage() + ")")
                .setCauseException(e)
                .buildWithContextFailing(routingContext)
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
              .onFailure(t -> TowerFailureHttpHandler.failRoutingContextWithTrace(t, routingContext, "Error while sending the registration email. Message: " + t.getMessage()))
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


      });
  }

  public void handleStep2EmailValidationLinkClick(RoutingContext ctx, AuthUser authUserAsClaims) {


    String listGuid;
    try {
      listGuid = authUserAsClaims.getListGuid();
    } catch (NullValueException e) {
      ctx.fail(new InternalException("No guid was in the claims for a user list registration"));
      return;
    }
    LocalDateTime optInTime = authUserAsClaims.getIssuedAt().toLocalDateTime();
    String optInIp;
    try {
      optInIp = authUserAsClaims.getOriginClientIp();
    } catch (NullValueException e) {
      LOGGER.error("The opt-in ip of the Jwt Claims is null");
      optInIp = "";
    }

    String subjectEmail = authUserAsClaims.getSubjectEmail();
    BMailInternetAddress bMailInternetAddress;
    try {
      bMailInternetAddress = BMailInternetAddress.of(subjectEmail);
    } catch (AddressException e) {
      TowerFailureException
        .builder()
        .setMessage("The AUTH subject email (" + subjectEmail + ") is not valid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .buildWithContextFailingTerminal(ctx);
      return;
    }

    Guid listGuidObject;
    try {
      listGuidObject = this.getApp().getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      TowerFailureException
        .builder()
        .setMessage("The list guid (" + listGuid + ") is not valid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .buildWithContextFailingTerminal(ctx);
      return;
    }

    String finalOptInIp = optInIp;

    AuthProvider authProvider = getApp().getAuthProvider();

    authProvider
      .getAuthUserForSessionByEmail(bMailInternetAddress, authUserAsClaims.getAudience())
      .onFailure(ctx::fail)
      .onSuccess(authUserForSession -> {
        Future<AuthUser> futureFinaleAuthSessionUser;
        if (authUserForSession != null) {
          futureFinaleAuthSessionUser = Future.succeededFuture(authUserForSession);
        } else {
          futureFinaleAuthSessionUser = authProvider.insertUserFromLoginAuthUserClaims(authUserAsClaims, ctx, this);
        }
        futureFinaleAuthSessionUser
          .onFailure(ctx::fail)
          .onSuccess(finalAuthSessionUser -> registerUserToList(ctx, listGuidObject, authProvider.toBaseModelUser(finalAuthSessionUser), optInTime, finalOptInIp, ListUserSource.EMAIL)
            .onFailure(ctx::fail)
            .onSuccess(registration -> {
              if (ctx.user() == null) {
                addRegistrationConfirmationCookieData(ctx, registration);
                UriEnhanced redirectUri = getRegistrationConfirmationOperationPath(registration);
                new AuthContext(this, ctx, finalAuthSessionUser, AuthState.createEmpty())
                  .redirectViaHttp(redirectUri)
                  .authenticateSession();
              }
            })
          );
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
  public static Future<ApiResponse<String>> handleStep0RegistrationForm(EraldyApiApp apiApp, RoutingContext routingContext, Guid listGuid) {


    Vertx vertx = routingContext.vertx();
    return apiApp.getListProvider()
      .getListByGuidObject(listGuid)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(list -> {
        if (list == null) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setName("The list was not found")
              .setMessage("The list <mark>" + listGuid + "</mark> was not found.")
              .setMimeToHtml()
              .buildWithContextFailing(routingContext)
          );
        }
        Map<String, Object> variables = new HashMap<>();
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
      .getListUserByGuidHash(registrationGuid)
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
   * Handle when a user is authenticated via OAuth.
   * This is a handler filter function, meaning that the authentication should continue.
   * <p>
   * If the user authenticate via a list, we register
   * the user to the list
   */
  public Handler<AuthContext> handleStepOAuthAuthentication() {
    return authContext -> {

      String listGuid = authContext.getAuthState().getListGuid();
      if (listGuid == null) {
        // no list in registration context, we continue
        authContext.next();
        return;
      }

      /**
       * A list in auth context, we register the user
       */
      Guid listGuidObject;
      try {
        listGuidObject = this.getApp().getListProvider().getGuidObject(listGuid);
      } catch (CastException e) {
        TowerFailureException
          .builder()
          .setMessage("The list guid in the Oauth context (" + listGuid + ") is not valid")
          .setCauseException(e)
          .buildWithContextFailingTerminal(authContext.getRoutingContext());
        return;
      }

      AuthUser authUser = authContext.getAuthUser();
      RoutingContext ctx = authContext.getRoutingContext();

      /**
       * A list registration
       */
      LocalDateTime optInTime = LocalDateTime.now();
      String optInIp;
      try {

        optInIp = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
      } catch (NotFoundException e) {
        LOGGER.warn("Oauth List registration: The remote ip client could not be found. Error: " + e.getMessage());
        optInIp = "";
      }

      User user = this.getApp().getAuthProvider().toBaseModelUser(authUser);
      this.registerUserToList(ctx, listGuidObject, user, optInTime, optInIp, ListUserSource.OAUTH)
        .onFailure(err -> authContext.getRoutingContext().fail(err))
        .onSuccess(registration -> authContext.next());
    };
  }

}
