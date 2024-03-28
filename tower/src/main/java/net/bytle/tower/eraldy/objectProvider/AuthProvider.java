package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.*;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.exception.NotSignedInOrganizationUser;
import net.bytle.tower.eraldy.auth.AuthClientScope;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.analytics.event.SignUpEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.WebFlow;

import java.lang.reflect.InvocationTargetException;
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
  private OrganizationUser getSignedInOrganizationalUserOrThrows(RoutingContext routingContext) throws NotFoundException, NotSignedInOrganizationUser {
    AuthUser authUser = this.getSignedInAuthUser(routingContext);
    return toModelUser(authUser, OrganizationUser.class);
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
    return AuthUser.createUserFromJsonClaims(user.principal().mergeIn(user.attributes()));


  }

  private <T extends User> T toModelUser(AuthUser authUser, Class<T> userClass) throws NotSignedInOrganizationUser {
    Realm realm = new Realm();
    /**
     * Audience guid may be null when we get an Oauth user
     * from an external oauth provider for instance
     */
    String audience = authUser.getAudience();
    if (audience != null) {
      try {
        realm.setGuid(audience);
        Guid realmGuid = this.apiApp.getRealmProvider().getGuidFromHash(audience);
        realm.setLocalId(realmGuid.getRealmOrOrganizationId());
      } catch (CastException e) {
        throw new InternalException("The audience value (" + audience + ") is not a valid realm guid", e);
      }
    }
    String audienceHandle = authUser.getAudienceHandle();
    if (audienceHandle == null && audience == null) {
      throw new InternalException("The audience and the audience handle values should not be together null");
    }
    realm.setHandle(audienceHandle);

    try {
      this.apiApp.getOrganizationUserProvider().checkOrganizationUserRealmId(userClass, realm.getLocalId());
    } catch (AssertionException e) {
      throw new NotSignedInOrganizationUser(e);
    }


    T userEraldy;
    try {
      userEraldy = userClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new InternalException("Unable to create a user instance", e);
    }
    userEraldy.setRealm(realm);
    String subjectGivenName = authUser.getSubjectGivenName();
    userEraldy.setGivenName(subjectGivenName);
    String subject = authUser.getSubject();
    String subjectEmail = authUser.getSubjectEmail();
    if (subject == null && subjectEmail == null) {
      throw new InternalException("The subject and the subject email values should not be together null");
    }
    userEraldy.setEmail(subjectEmail);
    if (subject != null) {
      /**
       * when retrieving an external Auth User from a social provider,
       * we don't have any subject
       */
      userEraldy.setGuid(subject);
      try {
        Guid guid = this.apiApp.getUserProvider().getGuidFromHash(subject);
        userEraldy.setLocalId(guid.validateRealmAndGetFirstObjectId(realm.getLocalId()));
      } catch (CastException e) {
        throw new InternalException(e);
      }
    }
    String subjectFamilyName = authUser.getSubjectFamilyName();
    String subjectFullName = subjectGivenName;
    if (subjectFullName != null && subjectFamilyName != null) {
      subjectFullName += " " + subjectFamilyName;
    }
    userEraldy.setFamilyName(subjectFullName);
    userEraldy.setBio(authUser.getSubjectBio());
    userEraldy.setLocation(authUser.getSubjectLocation());
    userEraldy.setWebsite(authUser.getSubjectBlog());
    userEraldy.setAvatar(authUser.getSubjectAvatar());
    return userEraldy;
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
      return toModelUser(authUser, User.class);
    } catch (NotSignedInOrganizationUser e) {
      // the exception is for an organizational user
      // it should not happen as we ask a model user
      throw new InternalException("The auth user could ne be retrieved as base user", e);
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
      return toModelUser(authUser, User.class);
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
  public Future<OrganizationUser> getSignedInOrganizationalUser(RoutingContext routingContext) {
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
   * @param authUserScope      - the scopes (permissions) - not implemented for now
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
   * @param routingContext the request context
   * @param realmId - the realm id
   * @param authUserScope - the scope
   * @return the realm
   */
  public Future<Realm> getRealmByLocalIdWithAuthorizationCheck(Long realmId, AuthUserScope authUserScope, RoutingContext routingContext) {
    return this.checkRealmAuthorization(routingContext,realmId, authUserScope)
      .compose(futureRealmId-> this.apiApp.getRealmProvider()
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
  public Future<AuthUser> getAuthUserForSessionByPasswordNotNull(String userEmail, String userPassword, Realm realm) {

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

    return this.apiApp.getUserProvider()
      .getUserByEmail(email, realmIdentifier)
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The user (" + email + "," + realmIdentifier + ") does not exist")
              .build()
          );
        }
        return toAuthUserForSession(userInDb)
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
   *
   * @param email - the email
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
  public <T extends User> AuthUser toAuthUser(T user) {
    AuthUser authUserClaims = new AuthUser();
    authUserClaims.setSubject(user.getGuid());
    authUserClaims.setSubjectHandle(user.getHandle());
    authUserClaims.setSubjectEmail(user.getEmail());
    authUserClaims.setRealmGuid(user.getRealm().getGuid());
    authUserClaims.setRealmHandle(user.getRealm().getHandle());
    if (user instanceof OrganizationUser) {
      Organization organization = ((OrganizationUser) user).getOrganization();
      // An organization user object is
      // a Eraldy user with or without an organization
      if (organization != null) {
        authUserClaims.setOrganizationGuid(organization.getGuid());
        authUserClaims.setOrganizationHandle(organization.getHandle());
      }
    }
    return authUserClaims;
  }


  /**
   * @param authUserAsClaims - the claims as auth user
   * @param routingContext   - the routing context for analytics (Maybe null when loading user without HTTP call, for instance for test
   * @param webFlow - the flow that creates this user
   * @return a user suitable
   */
  public Future<AuthUser> insertUserFromLoginAuthUserClaims(AuthUser authUserAsClaims, RoutingContext routingContext, WebFlow webFlow) {
    User user = toBaseModelUser(authUserAsClaims);
    return this.apiApp
      .getUserProvider()
      .insertUser(user)
      .compose(insertedUser -> toAuthUserForSession(user)
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

  }

  /**
   * @param user - the user to transform in auth user (This is a normal user build even if the user is a {@link OrganizationUser})
   * @return an auth user suitable to be put in a session (ie with role and permission)
   */
  private Future<AuthUser> toAuthUserForSession(User user) {

    AuthUser authUser = toAuthUser(user);
    Future<List<Realm>> futureRealmOwnerList;
    Future<OrganizationUser> futureOrgaUser;
    if (user instanceof OrganizationUser) {
      OrganizationUser orgUser = (OrganizationUser) user;
      futureRealmOwnerList = this.apiApp.getRealmProvider().getRealmsForOwner(orgUser);
      futureOrgaUser = this.apiApp.getOrganizationUserProvider().getOrganizationUserEnrichedWithOrganizationDataEventually(orgUser);
    } else {
      futureRealmOwnerList = Future.succeededFuture();
      futureOrgaUser = Future.succeededFuture();
    }

    return Future.all(futureRealmOwnerList, futureOrgaUser)
      .compose(
        res -> {
          List<Realm> realmList = res.resultAt(0);
          OrganizationUser orgaUser = res.resultAt(1);
          if (realmList != null) {
            List<Long> realmListLongId = realmList.stream()
              .map(Realm::getLocalId)
              .collect(Collectors.toList());
            authUser.put(REALMS_ID_KEY, realmListLongId);
          }
          if (orgaUser != null) {
            Organization organization = orgaUser.getOrganization();
            if (organization != null) {
              authUser.setOrganizationGuid(organization.getGuid());
              authUser.setOrganizationHandle(organization.getHandle());
            }
          }
          return Future.succeededFuture(authUser);
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
        return this.apiApp
          .getUserProvider()
          .patchUserIfPropertyValueIsNull(userInDb, toBaseModelUser(authUserClaims))
          .compose(patchUser -> toAuthUserForSession(patchUser)
            .compose(Future::succeededFuture)
          );
      });
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
    AuthUser authUser = toAuthUser(userToLogin);
    return AuthJwtClaims.createFromAuthUser(authUser);
  }

  public <T extends User> AnalyticsUser toAnalyticsUser(T user) {
    AnalyticsUser analyticsUser = new AnalyticsUser();
    analyticsUser.setGuid(user.getGuid());
    // user.getHandle(), handle is email
    analyticsUser.setEmail(user.getEmail());
    analyticsUser.setGivenName(user.getGivenName());
    analyticsUser.setFamilyName(user.getFamilyName());
    analyticsUser.setAvatar(user.getAvatar());
    analyticsUser.setRealmGuid(user.getRealm().getGuid());
    analyticsUser.setRealmHandle(user.getRealm().getHandle());
    if (user instanceof OrganizationUser) {
      Organization organization = ((OrganizationUser) user).getOrganization();
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
    if (!this.apiApp.getEraldyModel().getRealmLocalId().equals(authClient.getApp().getRealm().getLocalId())) {
      throw new NotAuthorizedException();
    }
  }
}
