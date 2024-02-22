package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.*;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.auth.AuthClientScope;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.util.Guid;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
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

  private static final String REGISTRATION_GUID_PARAM = ":guid";

  private final ListRegistrationEmailCallback callback;

  public ListRegistrationFlow(EraldyApiApp eraldyApiApp) {
    super(eraldyApiApp);
    this.callback = new ListRegistrationEmailCallback(this);

  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  @Override
  public FlowType getFlowType() {
    return FlowType.LIST_REGISTRATION;
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
  public Future<ListUser> createListUserEntry(RoutingContext ctx, Guid listGuid, User user, LocalDateTime optInTime, String optInIp, ListUserSource registrationFlow) {

    return this.getApp()
      .getListProvider()
      .getListByGuidObject(listGuid)
      .recover(err -> Future.failedFuture(new InternalException(err)))
      .compose(list -> {
        ListUser listUser = new ListUser();
        listUser.setList(list);
        listUser.setUser(user);
        listUser.setStatus(ListUserStatus.OK);
        listUser.setInOptInTime(optInTime);
        listUser.setInOptInConfirmationTime(DateTimeUtil.getNowInUtc());
        listUser.setInOptInIp(optInIp);
        try {
          String realRemoteClient = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
          listUser.setInOptInConfirmationIp(realRemoteClient);
        } catch (NotFoundException e) {
          LOGGER.warn("List registration validation: The remote ip client could not be found. Error: " + e.getMessage());
        }
        listUser.setInSourceId(registrationFlow);
        return this
          .getApp()
          .getListRegistrationProvider()
          .upsertListUser(listUser)
          .recover(err -> Future.failedFuture(new InternalException(err)))
          .compose(Future::succeededFuture);
      });
  }


  /**
   * Handle the post list registration
   *
   * @param routingContext              - the routing context
   * @param listUserPostBody - the post data
   */
  public Future<Void> handleStep1SendingValidationEmail(RoutingContext routingContext, String listGuidHash, ListUserPostBody listUserPostBody) {

    /**
     * Check authorization
     */
    AuthClient authClient = this.getApp().getAuthClientProvider().getRequestingClient(routingContext);
    AuthClientScope listRegistration = AuthClientScope.LIST_ADD_USER_FLOW;
    try {
      this.getApp().getAuthProvider().checkClientAuthorization(authClient, listRegistration);
    } catch (NotAuthorizedException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setMessage("You don't have any permission to " + listRegistration.getHumanActionName())
        .buildWithContextFailing(routingContext)
      );
    }

    /**
     * Email validation
     */
    BMailInternetAddress validatedEmailAddress;
    try {
      validatedEmailAddress = BMailInternetAddress.of(listUserPostBody.getUserEmail());
    } catch (AddressException e) {
      throw ValidationException.create("The email is not valid. Error: " + e.getMessage(), "userEmail", listUserPostBody.getUserEmail());
    }


    /**
     * We validate the publication id value
     * (not against the database)
     */
    if (listGuidHash == null) {
      return Future.failedFuture(IllegalArgumentExceptions.createWithInputNameAndValue("List guid should not be null", "listIdentifier", null));
    }

    return getApp().getListProvider()
      .getListByGuidHashIdentifier(listGuidHash)
      .compose(listItem -> {

        User user = new User();
        user.setEmail(validatedEmailAddress.toNormalizedString());
        Realm listRealm = listItem.getRealm();
        user.setRealm(listRealm);

        AuthJwtClaims jwtClaims = getApp().getAuthProvider()
          .toJwtClaims(user)
          .addRequestClaims(routingContext)
          .setListGuid(listGuidHash)
          .setRedirectUri(listUserPostBody.getRedirectUri());

        User listOwnerUser = ListProvider.getOwnerUser(listItem);
        SmtpSender sender = UsersUtil.toSenderUser(listOwnerUser);
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

        /**
         * Add the calling client id
         */
        Map<String, String> clientCallbackQueryProperties = new HashMap<>();
        clientCallbackQueryProperties.put(AuthQueryProperty.CLIENT_ID.toString(), authClient.getGuid());

        BMailTransactionalTemplate letter = getApp()
          .getUserListRegistrationFlow()
          .getCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, sender, subscriberRecipientName, jwtClaims, clientCallbackQueryProperties)
          .setPreview("Validate your registration to the list `" + listItem.getName() + "`")
          .addIntroParagraph(
            "I just got a subscription request to the list <mark>" + listItem.getName() + "</mark> with your email." +
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

            String mailSubject = "Registration validation to the list `" + listItem.getName() + "`";
            TowerSmtpClient towerSmtpClient = this.getApp().getHttpServer().getServer().getSmtpClient();

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
                String title = "The user (" + subscriberAddressWithName + ") received a validation email for the list (" + listItem.getHandle() + ").";
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

  /**
   * Handle when the user clicks on the link in the email
   * @param ctx - the context
   * @param jwtClaims - the claims received
   */
  public void handleStep2EmailValidationLinkClick(RoutingContext ctx, AuthJwtClaims jwtClaims) {


    String listGuid;
    try {
      listGuid = jwtClaims.getListGuid();
    } catch (NullValueException e) {
      ctx.fail(new InternalException("No guid was in the claims for a user list registration"));
      return;
    }

    LocalDateTime optInTime = jwtClaims.getIssuedAt().toLocalDateTime();
    String optInIp;
    try {
      optInIp = jwtClaims.getOriginClientIp();
    } catch (NullValueException e) {
      LOGGER.error("The opt-in ip of the Jwt Claims is null");
      optInIp = "";
    }

    String subjectEmail = jwtClaims.getSubjectEmail();
    EmailAddress bMailInternetAddress;
    try {
      bMailInternetAddress = EmailAddress.of(subjectEmail);
    } catch (EmailCastException e) {
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
      .getAuthUserForSessionByEmail(bMailInternetAddress, jwtClaims.getAudience())
      .onFailure(ctx::fail)
      .onSuccess(authUserForSession -> {
        Future<AuthUser> futureFinaleAuthSessionUser;
        if (authUserForSession != null) {
          futureFinaleAuthSessionUser = Future.succeededFuture(authUserForSession);
        } else {
          futureFinaleAuthSessionUser = authProvider.insertUserFromLoginAuthUserClaims(jwtClaims, ctx, this);
        }
        futureFinaleAuthSessionUser
          .onFailure(ctx::fail)
          .onSuccess(finalAuthSessionUser -> createListUserEntry(ctx, listGuidObject, authProvider.toBaseModelUser(finalAuthSessionUser), optInTime, finalOptInIp, ListUserSource.EMAIL)
            .onFailure(ctx::fail)
            .onSuccess(listUser -> {
              String jwtRedirectUri = jwtClaims.getRedirectUri().toString();
              String registrationConfirmationOperationPath = jwtRedirectUri.replace(REGISTRATION_GUID_PARAM, listUser.getGuid());
              UriEnhanced redirectUri;
              try {
                redirectUri = UriEnhanced.createFromString(registrationConfirmationOperationPath);
              } catch (IllegalStructure e) {
                TowerFailureException.builder()
                  .setMessage("The redirect uri (" + registrationConfirmationOperationPath + ") is not valid")
                  .setCauseException(e)
                  .buildWithContextFailingTerminal(ctx);
                return;
              }

              /**
               * Analytics Claims
               */
              jwtClaims.setAppGuid(listUser.getList().getApp().getGuid());
              jwtClaims.setAppHandle(listUser.getList().getApp().getHandle());
              jwtClaims.setRealmGuid(listUser.getList().getApp().getRealm().getGuid());
              jwtClaims.setRealmHandle(listUser.getList().getApp().getRealm().getHandle());

              /**
               * Authenticate
               */
              getApp().getAuthNContextManager()
                .newAuthNContext(ctx, this, finalAuthSessionUser, OAuthState.createEmpty(), jwtClaims)
                .redirectViaHttp(redirectUri)
                .authenticateSession();
            })
          );
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
  public Handler<AuthNContext> handleStepOAuthAuthentication() {
    return authContext -> {

      OAuthState oAuthState = authContext.getOAuthState();
      String listGuid = oAuthState.getListGuid();
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
      this.createListUserEntry(ctx, listGuidObject, user, optInTime, optInIp, ListUserSource.OAUTH)
        .onFailure(err -> authContext.getRoutingContext().fail(err))
        .onSuccess(listUser -> {
          /**
           * Update the confirmation URL
           * with the list user guid
           */
          UriEnhanced redirectUri = oAuthState.getRedirectUri();
          if (redirectUri == null) {
            TowerFailureException
              .builder()
              .setMessage("The redirect uri was not found in the OAuth State object")
              .buildWithContextFailingTerminal(authContext.getRoutingContext());
            return;
          }
          String registrationConfirmationOperationPath = redirectUri.getPath().replace(REGISTRATION_GUID_PARAM, listUser.getGuid());
          redirectUri.setPath(registrationConfirmationOperationPath);
          oAuthState.setRedirectUri(redirectUri);
          /**
           * Next handler
           */
          authContext.next();
        });
    };
  }

  @Override
  public Future<Void> mount() {
    /**
     * Add the user list registration callback
     */
    Router router = this.getApp().getHttpServer().getRouter();
    this
      .getCallback()
      .addCallback(router);
    return super.mount();
  }
}
