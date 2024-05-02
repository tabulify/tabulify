package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.*;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.auth.AuthClientScope;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.graphql.pojo.input.ListUserInputProps;
import net.bytle.tower.eraldy.model.openapi.ListUser;
import net.bytle.tower.eraldy.model.openapi.ListUserPostBody;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.type.Handle;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.SmtpSender;
import net.bytle.vertx.flow.WebFlowAbs;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
   * @param listUserSource - the flow used to register the user to the list
   */
  public Future<ListUser> createListUserEntry(RoutingContext ctx, ListGuid listGuid, User user, LocalDateTime optInTime, InetAddress optInIp, ListUserSource listUserSource) {

    return this.getApp()
      .getListProvider()
      .getListByGuidObject(listGuid)
      .recover(err -> Future.failedFuture(new InternalException(err)))
      .compose(list -> {
        ListUserInputProps listUserInputProps = new ListUserInputProps();
        listUserInputProps.setInOptInTime(optInTime);
        listUserInputProps.setInOptInConfirmationTime(DateTimeService.getNowInUtc());
        listUserInputProps.setInOptInIp(optInIp);
        try {
          String realRemoteClient = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
          try {
            listUserInputProps.setInOptInConfirmationIp(Address.getByAddress(realRemoteClient));
          } catch (UnknownHostException e) {
            LOGGER.warn("List registration validation: The remote ip client value (" + realRemoteClient + ") is not valid. Error: " + e.getMessage());
          }
        } catch (NotFoundException e) {
          LOGGER.warn("List registration validation: The remote ip client could not be found. Error: " + e.getMessage());
        }
        listUserInputProps.setInListUserSource(listUserSource);
        return this
          .getApp()
          .getListUserProvider()
          .insertListUser(user, list, listUserInputProps)
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
     * We validate the publication id value
     * (not against the database)
     */
    if (listGuidHash == null) {
      return Future.failedFuture(IllegalArgumentExceptions.createWithInputNameAndValue("List guid should not be null", "listIdentifier", null));
    }

    ListGuid listGuid;
    try {
      listGuid = this.getApp().getJackson().getDeserializer(ListGuid.class).deserialize(listGuidHash);
    } catch (CastException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The listGuid (" + listGuidHash + ") is not valid")
          .buildWithContextFailing(routingContext)
      );
    }

    return getApp().getListProvider()
      .getListByGuidObject(listGuid)
      .compose(listObject -> {

        User user = new User();
        user.setEmailAddress(listUserPostBody.getUserEmail());
        Realm listRealm = listObject.getApp().getRealm();
        user.setRealm(listRealm);

        AuthJwtClaims jwtClaims = getApp().getAuthProvider()
          .toJwtClaims(user)
          .addRequestClaims(routingContext)
          .setListGuid(listGuidHash)
          .setRedirectUri(listUserPostBody.getRedirectUri());

        User listOwnerUser = ListProvider.getOwnerUser(listObject);
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
        String clientIdHash = this.getApp().getJackson().getSerializer(CliGuid.class).serialize(authClient.getGuid());
        clientCallbackQueryProperties.put(AuthQueryProperty.CLIENT_ID.toString(), clientIdHash);

        BMailTransactionalTemplate letter = getApp()
          .getUserListRegistrationFlow()
          .getCallback()
          .getCallbackTransactionalEmailTemplateForClaims(routingContext, sender, subscriberRecipientName, jwtClaims, clientCallbackQueryProperties)
          .setPreview("Validate your registration to the list `" + listObject.getName() + "`")
          .addIntroParagraph(
            "I just got a subscription request to the list <mark>" + listObject.getName() + "</mark> with your email." +
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

            String mailSubject = "Registration validation to the list `" + listObject.getName() + "`";
            TowerSmtpClientService towerSmtpClientService = this.getApp().getHttpServer().getServer().getSmtpClient();

            String ownerEmailAddressInRfcFormat;
            try {
              ownerEmailAddressInRfcFormat = BMailInternetAddress.of(listOwnerUser.getEmailAddress(), listOwnerUser.getGivenName()).toString();
            } catch (AddressException e) {
              return Future.failedFuture(TowerFailureException.builder().setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
                .setMessage("The list owner email (" + listOwnerUser.getEmailAddress() + ") is not good (" + e.getMessage() + ")")
                .setCauseException(e)
                .buildWithContextFailing(routingContext)
              );
            }

            String subscriberAddressWithName;
            try {
              subscriberAddressWithName = BMailInternetAddress.of(user.getEmailAddress(), subscriberRecipientName).toString();
            } catch (AddressException e) {
              return Future.failedFuture(TowerFailureException
                .builder()
                .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
                .setMessage("The subscriber email (" + user.getEmailAddress() + ") is not good (" + e.getMessage() + ")")
                .setCauseException(e)
                .buildWithContextFailing(routingContext)
              );
            }

            MailClient mailClientForListOwner = towerSmtpClientService
              .getVertxMailClientForSenderWithSigning(listOwnerUser.getEmailAddress());

            MailMessage registrationEmail = towerSmtpClientService
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
                String title = "The user (" + subscriberAddressWithName + ") received a validation email for the list (" + listObject.getHandle() + ").";
                MailMessage ownerFeedbackEmail = towerSmtpClientService
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
    InetAddress optInIp = null;
    try {
      String optInIpString = jwtClaims.getOriginClientIp();
      try {
        optInIp = Address.getByAddress(optInIpString);
      } catch (UnknownHostException e) {
        LOGGER.error("The opt-in ip of the Jwt Claims ("+optInIpString+") is not valid. Error: "+e.getMessage(),e);
      }
    } catch (NullValueException e) {
      LOGGER.error("The opt-in ip of the Jwt Claims is null");
    }

    AuthUser authUser = jwtClaims.toAuthUser();
    String subjectEmail = authUser.getSubjectEmail();
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

    ListGuid listGuidObject;
    try {
      listGuidObject = this.getApp().getJackson().getDeserializer(ListGuid.class).deserialize(listGuid);
    } catch (CastException e) {
      TowerFailureException
        .builder()
        .setMessage("The list guid (" + listGuid + ") is not valid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .buildWithContextFailingTerminal(ctx);
      return;
    }

    InetAddress finalOptInIp = optInIp;

    AuthProvider authProvider = getApp().getAuthProvider();

    authProvider
      .getAuthUserForSessionByEmail(bMailInternetAddress, authUser.getAudience())
      .onFailure(ctx::fail)
      .onSuccess(authUserForSession -> {
        Future<AuthUser> futureFinaleAuthSessionUser;
        if (authUserForSession != null) {
          futureFinaleAuthSessionUser = Future.succeededFuture(authUserForSession);
        } else {
          futureFinaleAuthSessionUser = authProvider.insertUserFromLoginAuthUserClaims(authUser, ctx, this);
        }
        futureFinaleAuthSessionUser
          .onFailure(ctx::fail)
          .onSuccess(finalAuthSessionUser -> createListUserEntry(ctx, listGuidObject, authProvider.toModelUser(finalAuthSessionUser), optInTime, finalOptInIp, ListUserSource.EMAIL)
            .onFailure(ctx::fail)
            .onSuccess(listUser -> {
              String jwtRedirectUri = jwtClaims.getRedirectUri().toString();
              String listUserGuidHash = this.getApp().getJackson().getSerializer(ListUserGuid.class).serialize(listUser.getGuid());
              String registrationConfirmationOperationPath = jwtRedirectUri.replace(REGISTRATION_GUID_PARAM, listUserGuidHash);
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
              JacksonMapperManager jacksonMapper = this.getApp().getHttpServer().getServer().getJacksonMapperManager();
              jwtClaims.setAppGuid(jacksonMapper.getSerializer(AppGuid.class).serialize(listUser.getList().getApp().getGuid()));
              jwtClaims.setAppHandle(jacksonMapper.getSerializer(Handle.class).serialize(listUser.getList().getApp().getHandle()));

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
      ListGuid listGuidObject;
      try {
        listGuidObject = this.getApp().getJackson().getDeserializer(ListGuid.class).deserialize(listGuid);
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
      InetAddress optInIp = null;
      try {
        String optInIpString = HttpRequestUtil.getRealRemoteClientIp(ctx.request());
        try {
          optInIp = Address.getByAddress(optInIpString);
        } catch (UnknownHostException e) {
          LOGGER.warn("Oauth List registration: The remote ip client ("+optInIpString+") is not valid. Error: " + e.getMessage(),e);
        }
      } catch (NotFoundException e) {
        LOGGER.warn("Oauth List registration: The remote ip client could not be found. Error: " + e.getMessage());
      }

      User user = this.getApp().getAuthProvider().toModelUser(authUser);
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
          String listUserGuidHash = this.getApp().getJackson().getSerializer(ListUserGuid.class).serialize(listUser.getGuid());
          String registrationConfirmationOperationPath = redirectUri.getPath().replace(REGISTRATION_GUID_PARAM, listUserGuidHash);
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
