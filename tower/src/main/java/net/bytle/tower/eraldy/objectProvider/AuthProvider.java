package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotAuthorizedException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.exception.NotSignedInOrganizationUser;
import net.bytle.tower.eraldy.auth.AuthClientScope;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.analytics.event.SignUpEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.WebFlow;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides function to:
 * * build and get the signed-in user for the session
 * * get login claims
 * * get and authorization functions
 */
public class AuthProvider {

  /**
   * A realms local id set that the user may manage.
   */
  private static final String REALMS_ID_KEY = "realms_id";
  private final EraldyApiApp apiApp;


  public AuthProvider(EraldyApiApp eraldyApiApp) {

    this.apiApp = eraldyApiApp;

  }

  /**
   * A private utility function that returns the Auth User has organizational user
   * You want to use {@link #getSignedInOrganizationalUser(RoutingContext)}
   *
   * @param routingContext - the routing context
   * @return the signed organizational user
   * @throws NotFoundException           - if the user is not found
   * @throws NotSignedInOrganizationUser - if the user is not an organization user
   */
  private OrgaUser getSignedInOrganizationalUserOrThrows(RoutingContext routingContext) throws NotFoundException, NotSignedInOrganizationUser {
    AuthUser authUser = this.getSignedInAuthUser(routingContext);
    return toModelUser(authUser);
  }

  /**
   * @param ctx - the context
   * @return the authenticated user
   * @throws NotFoundException if not found
   */
  public AuthUser getSignedInAuthUser(RoutingContext ctx) throws NotFoundException {

    /**
     * Why the auth user is not in a {@link net.bytle.tower.eraldy.model.openapi.User} format
     * Because:
     * * AuthUser stores the authorization and role
     * * To store the user in a session, it should be serializable
     * (ie wrapped as in the {@link io.vertx.ext.web.handler.impl.UserHolder})
     * As of today, only the {@link AuthUser} via the vertx {@link io.vertx.ext.auth.User}
     * that is a Json serializable object
     */
    io.vertx.ext.auth.User user = ctx.user();
    if (user == null) {
      throw new NotFoundException();
    }
    return AuthUser.createFromUser(user);


  }

  /**
   * Return a valid model user from an authUser
   * The authUser should be a valid user in the database (not an oauth token)
   */
  private <T extends User> T toModelUser(AuthUser authUser) throws NotSignedInOrganizationUser {

    /**
     * Organization
     */
    String organizationGuidString = authUser.getOrganizationGuid();
    T user;
    if (organizationGuidString != null) {

      //noinspection unchecked
      user = (T) new OrgaUser();

      Guid orgaGuidObject;
      try {
        orgaGuidObject = this.apiApp.getOrganizationProvider().createGuidFromHash(organizationGuidString);
      } catch (CastException e) {
        throw new InternalException("The organization guid (" + organizationGuidString + ") is not valid", e);
      }
      Organization organization = new Organization();
      organization.setGuid(organizationGuidString);
      organization.setLocalId(orgaGuidObject.getRealmOrOrganizationId());
      organization.setHandle(authUser.getOrganizationHandle());
      ((OrgaUser) user).setOrganization(organization);

    } else {
      //noinspection unchecked
      user = (T) new User();
    }

    /**
     * Realm / Audience
     * <p>
     * First because it's in the user guid
     */
    Realm realm = new Realm();
    String audience = authUser.getAudience();
    if (audience == null) {
      throw new InternalException("The audience/realm guid should not be null for a user");
    }
    Guid realmGuid;
    try {
      realm.setGuid(audience);
      realmGuid = this.apiApp.getRealmProvider().getGuidFromHash(audience);
    } catch (CastException e) {
      throw new InternalException("The audience value (" + audience + ") is not a valid realm guid", e);
    }

    long realmId = realmGuid.getRealmOrOrganizationId();
    realm.setLocalId(realmId);
    if (user instanceof OrgaUser && realmId != EraldyModel.REALM_LOCAL_ID) {
      throw new InternalException("An orga user should have the Eraldy realm, not the realm (" + realmId + ")");
    }
    String audienceHandle = authUser.getAudienceHandle();
    realm.setHandle(audienceHandle);
    user.setRealm(realm);

    /**
     * Email (We wrote the email, it should be good)
     */
    EmailAddress subjectEmail = EmailAddress.ofFailSafe(authUser.getSubjectEmail());
    user.setEmailAddress(subjectEmail);
    String subject = authUser.getSubject();
    if (subject == null) {
      throw new InternalException("The subject (user guid) values should not be null");
    }
    try {
      UserGuid guid = this.apiApp.getUserProvider().getGuidFromHash(subject);
      user.setLocalId(guid.getLocalId());
      user.setGuid(guid);
    } catch (CastException e) {
      throw new InternalException("The user guid is not valid", e);
    }

    /**
     * Other attributes
     */
    String subjectGivenName = authUser.getSubjectGivenName();
    user.setGivenName(subjectGivenName);

    String subjectFamilyName = authUser.getSubjectFamilyName();
    String subjectFullName = subjectGivenName;
    if (subjectFullName != null && subjectFamilyName != null) {
      subjectFullName += " " + subjectFamilyName;
    }
    user.setFamilyName(subjectFullName);
    user.setBio(authUser.getSubjectBio());
    user.setLocation(authUser.getSubjectLocation());
    user.setWebsite(authUser.getSubjectBlog());
    user.setAvatar(authUser.getSubjectAvatar());

    return user;

  }

  /**
   * A utility map to transform into a user to calculate
   * the identifier
   *
   * @param authUser - the auth user
   * @return a user with the ids
   */
  public User toBaseModelUser(AuthUser authUser) {

    try {
      return toModelUser(authUser);
    } catch (NotSignedInOrganizationUser e) {
      // the exception is for an organizational user
      // it should not happen as we ask a model user
      throw new InternalException("The auth user could not be retrieved as base user", e);
    }

  }

  public Future<User> getSignedInBaseUserOrFail(RoutingContext routingContext) {
    try {
      return Future.succeededFuture(this.getSignedInBaseUserOrThrow(routingContext));
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException
          .builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .build()
      );
    }
  }

  private User getSignedInBaseUserOrThrow(RoutingContext routingContext) throws NotFoundException {
    AuthUser authUser = this.getSignedInAuthUser(routingContext);
    try {
      return toModelUser(authUser);
    } catch (NotSignedInOrganizationUser e) {
      // the exception is for an organizational user
      // it should not happen as we ask a model user
      throw new InternalException("The auth user could ne be retrieved as base user", e);
    }
  }

  /**
   * Utility function to get the user as future
   * (ie fail the context if the user is not present or not the good one)
   * This function is used mostly in Api implementation interface
   * to not have to deal with the exception thrown of {@link #getSignedInOrganizationalUserOrThrows(RoutingContext)}
   *
   * @param routingContext - the routing context
   * @return a user or a failed future
   */
  public Future<OrgaUser> getSignedInOrganizationalUser(RoutingContext routingContext) {
    try {
      return Future.succeededFuture(this.apiApp.getAuthProvider().getSignedInOrganizationalUserOrThrows(routingContext));
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .setMessage("You should be logged in")
          .buildWithContextFailing(routingContext)
      );
    } catch (NotSignedInOrganizationUser e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
          .setMessage("You should be logged as an organizational user")
          .setCauseException(e)
          .buildWithContextFailing(routingContext)
      );
    }

  }

  /**
   * @param routingContext - the http context
   * @param realm          - the realm to authorize
   * @param authUserScope  - the scopes (permissions) - not implemented for now
   * @return the realm if authorize or a failure
   */
  public Future<Realm> checkRealmAuthorization(RoutingContext routingContext, Realm realm, AuthUserScope authUserScope) {

    return checkRealmAuthorization(routingContext, realm.getLocalId(), authUserScope)
      .compose(realmId -> Future.succeededFuture(realm));

  }

  public Future<Long> checkRealmAuthorization(RoutingContext routingContext, Long realmId, AuthUserScope authUserScope) {
    if (authUserScope.isPublic()) {
      return Future.succeededFuture(realmId);
    }
    return this.getSignedInAuthUserOrFail(routingContext)
      .compose(signedInUser -> {
        if (ApiKeyAuthenticationProvider.ROOT_AUTHORIZATION.match(signedInUser.getVertxUser())) {
          return Future.succeededFuture(realmId);
        }
        Set<Long> realmGuids = getManagedRealmsId(signedInUser);
        if (!realmGuids.contains(realmId)) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("Authenticated User (" + signedInUser + ") has no permission on the requested realm (" + realmId + "). Scope: " + authUserScope)
              .build()
          );
        }
        return Future.succeededFuture(realmId);
      });
  }

  /**
   * Utility class to get a realm from an id
   * and check the user authorization at the same time
   *
   * @param routingContext the request context
   * @param realmId        - the realm id
   * @param authUserScope  - the scope
   * @return the realm
   */
  public Future<Realm> getRealmByLocalIdWithAuthorizationCheck(Long realmId, AuthUserScope authUserScope, RoutingContext routingContext) {
    return this.checkRealmAuthorization(routingContext, realmId, authUserScope)
      .compose(futureRealmId -> this.apiApp.getRealmProvider()
        .getRealmFromLocalId(futureRealmId));
  }

  private Set<Long> getManagedRealmsId(AuthUser signedInUser) {
    return signedInUser.getSet(REALMS_ID_KEY, Long.class);
  }

  private Future<AuthUser> getSignedInAuthUserOrFail(RoutingContext routingContext) {
    try {
      return Future.succeededFuture(getSignedInAuthUser(routingContext));
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .build()
      );
    }
  }


  /**
   * @param userEmail    - the user email
   * @param userPassword - the password in clear
   * @param realm        - the realm
   * @return a user if the user handle, realm and password combination are good
   */
  public Future<AuthUser> getAuthUserForSessionByPasswordNotNull(EmailAddress userEmail, String userPassword, Realm realm) {

    return this.apiApp.getUserProvider()
      .getUserByPassword(userEmail, userPassword, realm)
      .compose(
        user -> {
          if (user == null) {
            return Future.failedFuture(
              TowerFailureException.builder()
                .setType(TowerFailureTypeEnum.NOT_FOUND_404)
                .build()
            );
          }
          return toAuthUserForSession(user)
            .compose(Future::succeededFuture);
        });

  }

  public Future<AuthUser> getAuthUserForSessionByEmailNotNull(EmailAddress email, String realmIdentifier) {

    return this.apiApp
      .getUserProvider()
      .getUserByEmail(email, realmIdentifier)
      .compose(user -> {
        if (user == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The user (" + email + "," + realmIdentifier + ") does not exist")
              .build()
          );
        }
        return toAuthUserForSession(user)
          .compose(Future::succeededFuture);
      });

  }

  public Future<AuthUser> getAuthUserForSessionByEmail(EmailAddress email, Realm realm) {

    return this.apiApp.getUserProvider()
      .getUserByEmail(email, realm)
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.succeededFuture();
        }
        return toAuthUserForSession(userInDb)
          .compose(Future::succeededFuture);
      });

  }

  /**
   * @param email           - the email
   * @param realmIdentifier - the realm
   * @return an auth user to be stored in a session or null
   */
  public Future<AuthUser> getAuthUserForSessionByEmail(EmailAddress email, String realmIdentifier) {

    return this.apiApp.getUserProvider()
      .getUserByEmail(email, realmIdentifier)
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.succeededFuture();
        }
        return toAuthUserForSession(userInDb)
          .compose(Future::succeededFuture);
      });

  }

  /**
   * @param user - the user
   * @return a base auth user (that can be used as claims in order to create a token or to create an auth user for a session)
   */
  public <T extends User> AuthUser.Builder toAuthUserBuilder(T user) {


    AuthUser.Builder authUserBuilder = AuthUser
      .builder()
      .setSubject(this.apiApp.getUserProvider().serializeUserGuid(user.getGuid()))
      .setSubjectEmail(user.getEmailAddress())
      .setRealmGuid(user.getRealm().getGuid())
      .setRealmHandle(user.getRealm().getHandle());
    if (user instanceof OrgaUser) {
      Organization organization = ((OrgaUser) user).getOrganization();
      // An organization user object is
      // a Eraldy user with or without an organization
      if (organization != null) {
        authUserBuilder.setOrganizationGuid(organization.getGuid());
        authUserBuilder.setOrganizationHandle(organization.getHandle());
      }
    }
    return authUserBuilder;
  }


  /**
   * @param authUserAsClaims - the claims as auth user
   * @param routingContext   - the routing context for analytics (Maybe null when loading user without HTTP call, for instance for test
   * @param webFlow          - the flow that creates this user
   * @return a user suitable
   */
  public Future<AuthUser> insertUserFromLoginAuthUserClaims(AuthUser authUserAsClaims, RoutingContext routingContext, WebFlow webFlow) {

    String realmGuid = authUserAsClaims.getRealmGuid();
    return this.apiApp
      .getRealmProvider()
      .getRealmFromIdentifier(realmGuid)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The realm (" + realmGuid + ") was not found")
            .build());
        }
        UserInputProps userInputProps;
        try {
          userInputProps = toUserInput(authUserAsClaims);
        } catch (EmailCastException e) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.BAD_STRUCTURE_422)
            .setMessage(e.getMessage())
            .setCauseException(e)
            .build());
        }
        return this.apiApp.getUserProvider()
          .insertUser(realm, userInputProps)
          .compose(insertedUser -> toAuthUserForSession(insertedUser)
            .compose(authUserForSession -> {
              SignUpEvent signUpEvent = new SignUpEvent();
              signUpEvent.getRequest().setFlowGuid(webFlow.getFlowType().getId().toString());
              signUpEvent.getRequest().setFlowHandle(webFlow.getFlowType().getHandle());
              this.apiApp
                .getHttpServer()
                .getServer()
                .getTrackerAnalytics()
                .eventBuilder(signUpEvent)
                .setAuthUser(authUserForSession)
                .setRoutingContext(routingContext)
                .processEvent();
              return Future.succeededFuture(authUserForSession);
            }));
      });


  }

  /**
   * @param user - the user to transform in auth user
   * @return an auth user suitable to be put in a session (ie with role and permission)
   */
  private <T extends User> Future<AuthUser> toAuthUserForSession(T user) {

    AuthUser.Builder authUserBuilder = toAuthUserBuilder(user);

    Future<List<Realm>> futureRealmOwnerList = Future.succeededFuture();
    if (user instanceof OrgaUser) {
      OrgaUser orgaUser = (OrgaUser) user;
      Organization organization = orgaUser.getOrganization();
      authUserBuilder.setOrganizationGuid(organization.getGuid());
      authUserBuilder.setOrganizationHandle(organization.getHandle());
      futureRealmOwnerList = this.apiApp.getRealmProvider().getRealmsForOwner(orgaUser);
    }
    return futureRealmOwnerList
      .compose(realmList -> {
        if (realmList != null) {
          List<Long> realmListLongId = realmList.stream()
            .map(Realm::getLocalId)
            .collect(Collectors.toList());
          authUserBuilder.put(REALMS_ID_KEY, realmListLongId);
        }
        return Future.succeededFuture(authUserBuilder.build());
      });

  }

  public Future<AuthUser> getAuthUserForSessionByClaims(AuthUser authUserClaims) {
    String subjectEmail = authUserClaims.getSubjectEmail();
    EmailAddress bMailInternetAddress;
    try {
      bMailInternetAddress = EmailAddress.of(subjectEmail);
    } catch (EmailCastException e) {
      return Future.failedFuture(TowerFailureException
        .builder()
        .setMessage("The AuthUser subject email (" + subjectEmail + ") is not valid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .build()
      );
    }
    return this.apiApp.getUserProvider()
      .getUserByEmail(bMailInternetAddress, authUserClaims.getAudience())
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.succeededFuture();
        }
        UserInputProps userInput;
        try {
          userInput = toUserInput(authUserClaims);
        } catch (EmailCastException e) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.BAD_STRUCTURE_422)
            .setMessage(e.getMessage())
            .setCauseException(e)
            .build());
        }
        return this.apiApp
          .getUserProvider()
          .updateUser(userInDb, userInput)
          .compose(patchUser -> toAuthUserForSession(patchUser)
            .compose(Future::succeededFuture)
          );
      });
  }

  private UserInputProps toUserInput(AuthUser authUserClaims) throws EmailCastException {
    UserInputProps userInputProps = new UserInputProps();
    String subjectEmail = authUserClaims.getSubjectEmail();
    EmailAddress emailAddress = EmailAddress.of(subjectEmail);
    userInputProps.setEmailAddress(emailAddress);
    userInputProps.setGivenName(authUserClaims.getSubjectGivenName());
    userInputProps.setFamilyName(authUserClaims.getSubjectFamilyName());
    userInputProps.setBio(authUserClaims.getSubjectBio());
    userInputProps.setAvatar(authUserClaims.getSubjectAvatar());
    userInputProps.setLocation(authUserClaims.getSubjectLocation());
    userInputProps.setWebsite(authUserClaims.getSubjectBlog());
    return userInputProps;
  }


  public Future<Void> checkOrgAuthorization(RoutingContext routingContext, String requestedOrgGuid, AuthUserScope authUserScope) {
    return this.getSignedInAuthUserOrFail(routingContext)
      .compose(signedInUser -> {
        String signedInUserGroup = signedInUser.getOrganizationGuid();
        if (signedInUserGroup == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("Authenticated User (" + signedInUser + ") is not an organizational user. Scope: " + authUserScope)
              .build()
          );
        }
        if (!signedInUserGroup.contains(requestedOrgGuid)) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("Authenticated User (" + signedInUser + ") has no permission on the requested organization (" + requestedOrgGuid + "). Scope: " + authUserScope)
              .build()
          );
        }
        return Future.succeededFuture();
      });
  }

  public Future<ListObject> checkListAuthorization(RoutingContext routingContext, ListObject list, AuthUserScope authUserScope) {
    return this.checkRealmAuthorization(routingContext, list.getRealm(), authUserScope)
      .compose(realm -> Future.succeededFuture(list));
  }

  public AuthJwtClaims toJwtClaims(User userToLogin) {

    return AuthJwtClaims.createFromAuthUser(toAuthUser(userToLogin));
  }

  public <T extends User> AnalyticsUser toAnalyticsUser(T user) {
    AnalyticsUser analyticsUser = new AnalyticsUser();
    analyticsUser.setGuid(this.apiApp.getUserProvider().serializeUserGuid(user.getGuid()));
    // note: handle is email
    analyticsUser.setEmail(user.getEmailAddress().toNormalizedString());
    analyticsUser.setGivenName(user.getGivenName());
    analyticsUser.setFamilyName(user.getFamilyName());
    analyticsUser.setAvatar(user.getAvatar());
    analyticsUser.setRealmGuid(user.getRealm().getGuid());
    analyticsUser.setRealmHandle(user.getRealm().getHandle());
    if (user instanceof OrgaUser) {
      Organization organization = ((OrgaUser) user).getOrganization();
      // An organization user object is
      // a Eraldy user with or without an organization
      if (organization != null) {
        analyticsUser.setOrganizationGuid(organization.getGuid());
        analyticsUser.setOrganizationHandle(organization.getHandle());
      }
    }
    return analyticsUser;
  }


  public void checkClientAuthorization(AuthClient authClient, AuthClientScope authUserScope) throws NotAuthorizedException {

    /**
     * Public Client Scopes (?)
     */
    if (authUserScope.isPublic()) {
      return;
    }
    /**
     * Technically all valid client should be able to
     * do the same as the member app.
     * Why? Because the member app proxy all client
     * We have only Eraldy client, we give the authorization to them
     * As of now, there is no notion of proxy by in the auth client
     */
    if (!authClient.getApp().getRealm().getLocalId().equals(EraldyModel.REALM_LOCAL_ID)) {
      throw new NotAuthorizedException();
    }
  }

  public AuthUser toAuthUser(User vertxUserToLogin) {
    return toAuthUserBuilder(vertxUserToLogin).build();
  }

}
