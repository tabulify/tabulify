package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.AssertionException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.exception.NotSignedInOrganizationUser;
import net.bytle.tower.eraldy.auth.AuthScope;
import net.bytle.tower.eraldy.event.SignUpEvent;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
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
    return AuthUser.createFromClaims(user.principal().mergeIn(user.attributes()));


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
   * @param authScope      - the scopes (permissions) - not implemented for now
   * @return the realm if authorize or a failure
   */
  public Future<Realm> checkRealmAuthorization(RoutingContext routingContext, Realm realm, AuthScope authScope) {

    return checkRealmAuthorization(routingContext, realm.getLocalId(), authScope)
      .compose(realmId -> Future.succeededFuture(realm));

  }

  public Future<Long> checkRealmAuthorization(RoutingContext routingContext, Long realmId, AuthScope authScope) {
    return this.getSignedInAuthUserOrFail(routingContext)
      .compose(signedInUser -> {
        Set<Long> realmGuids = getManagedRealmsId(signedInUser);
        if (!realmGuids.contains(realmId)) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("Authenticated User (" + signedInUser + ") has no permission on the requested realm (" + realmId + "). Scope: " + authScope)
              .build()
          );
        }
        return Future.succeededFuture(realmId);
      });
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

  public Future<AuthUser> getAuthUserForSessionByEmailNotNull(BMailInternetAddress email, String realmIdentifier) {

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

  public Future<AuthUser> getAuthUserForSessionByEmail(BMailInternetAddress email, String realmIdentifier) {

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
    authUserClaims.setAudience(user.getRealm().getGuid());
    authUserClaims.setAudienceHandle(user.getRealm().getHandle());
    if (user instanceof OrganizationUser) {
      Organization organization = ((OrganizationUser) user).getOrganization();
      // An organization user object is
      // a Eraldy user with or without an organization
      if (organization != null) {
        authUserClaims.setGroup(organization.getGuid());
      }
    }
    return authUserClaims;
  }


  /**
   * @param authUserAsClaims - the claims as auth user
   * @param routingContext   - the routing context for analytics (Maybe null when loading user without HTTP call, for instance for test
   * @param webFlow
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
          signUpEvent.setFlowId(webFlow.getFlowType().getValue());
          this.apiApp
            .getApexDomain()
            .getHttpServer()
            .getServer()
            .getTrackerAnalytics()
            .eventBuilderForServerEvent(signUpEvent)
            .setUser(authUserForSession)
            .setRoutingContext(routingContext)
            .addEventToQueue();
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
      futureRealmOwnerList = this.apiApp.getRealmProvider().getRealmsForOwner(orgUser, Realm.class);
      futureOrgaUser = this.apiApp.getOrganizationUserProvider().getOrganizationUserByUser(orgUser);
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
              authUser.setGroup(organization.getGuid());
            }
          }
          return Future.succeededFuture(authUser);
        });

  }

  public Future<AuthUser> getAuthUserForSessionByClaims(AuthUser authUserClaims) {
    String subjectEmail = authUserClaims.getSubjectEmail();
    BMailInternetAddress bMailInternetAddress;
    try {
      bMailInternetAddress = BMailInternetAddress.of(subjectEmail);
    } catch (AddressException e) {
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


  public Future<Void> checkOrgAuthorization(RoutingContext routingContext, String requestedOrgGuid, AuthScope authScope) {
    return this.getSignedInAuthUserOrFail(routingContext)
      .compose(signedInUser -> {
        String signedInUserGroup = signedInUser.getGroup();
        if (signedInUserGroup == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("Authenticated User (" + signedInUser + ") is not an organizational user. Scope: " + authScope)
              .build()
          );
        }
        if (!signedInUserGroup.contains(requestedOrgGuid)) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("Authenticated User (" + signedInUser + ") has no permission on the requested organization (" + requestedOrgGuid + "). Scope: " + authScope)
              .build()
          );
        }
        return Future.succeededFuture();
      });
  }

  public Future<ListItem> checkListAuthorization(RoutingContext routingContext, ListItem list, AuthScope authScope) {
    return this.checkRealmAuthorization(routingContext, list.getRealm(), authScope)
      .compose(realm -> Future.succeededFuture(list));
  }
}
