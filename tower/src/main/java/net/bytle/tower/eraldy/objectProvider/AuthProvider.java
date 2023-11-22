package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.AssertionException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.exception.NotSignedInOrganizationUser;
import net.bytle.tower.eraldy.auth.AuthPermission;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.AnalyticsEventName;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureStatusEnum;
import net.bytle.vertx.auth.AuthUser;

import java.lang.reflect.InvocationTargetException;

/**
 * Provide authenticated user and authorization functions
 */
public class AuthProvider {
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
     * Why not in a {@link net.bytle.tower.eraldy.model.openapi.User} format
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
    userEraldy.setFullName(subjectFullName);
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

  public Future<User> getSignedInBaseUser(RoutingContext routingContext) {
    try {
      return Future.succeededFuture(this.getSignedInBaseUserOrThrow(routingContext));
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException
          .builder()
          .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
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
          .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
          .setMessage("You should be logged in")
          .buildWithContextFailing(routingContext)
      );
    } catch (NotSignedInOrganizationUser e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setStatus(TowerFailureStatusEnum.NOT_AUTHORIZED_403)
          .setMessage("You should be logged as an organizational user")
          .setException(e)
          .buildWithContextFailing(routingContext)
      );
    }

  }

  @SuppressWarnings("unused")
  public Future<Realm> checkRealmAuthorization(Realm realm, AuthPermission authPermission) {

    boolean authorized = false;
    if (!authorized) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setStatus(TowerFailureStatusEnum.NOT_AUTHORIZED_403)
          .setMessage("Authenticated User has no permission on the requested realm (" + realm + ") for the permission (" + authPermission + ")")
          .build()
      );
    }
    return Future.succeededFuture(realm);

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
      .compose(user -> {
        if (user == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_FOUND_404)
              .build()
          );
        }
        return Future.succeededFuture(toAuthUserForSession(user));
      });

  }

  public Future<AuthUser> getAuthUserForSessionByEmailNotNull(String email, String realmIdentifier) {

    return this.apiApp.getUserProvider()
      .getUserByEmail(email, realmIdentifier)
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_FOUND_404)
              .setMessage("The user (" + email + "," + realmIdentifier + ")  send by mail, does not exist")
              .build()
          );
        }
        return Future.succeededFuture();
      });

  }

  public Future<AuthUser> getAuthUserForSessionByEmail(String email, String realmIdentifier) {

    return this.apiApp.getUserProvider()
      .getUserByEmail(email, realmIdentifier)
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.succeededFuture();
        }
        return Future.succeededFuture(this.toAuthUserForSession(userInDb));
      });

  }

  /**
   * @param user - the user
   * @return an auth user that can be used as claims in order to create a token
   */
  public AuthUser toAuthUserForLoginToken(User user) {
    AuthUser authUserClaims = new AuthUser();
    authUserClaims.setSubject(user.getGuid());
    authUserClaims.setSubjectHandle(user.getHandle());
    authUserClaims.setSubjectEmail(user.getEmail());
    authUserClaims.setAudience(user.getRealm().getGuid());
    authUserClaims.setAudienceHandle(user.getRealm().getHandle());
    return authUserClaims;
  }

  /**
   * @param authUserAsClaims - the claims as auth user
   * @param routingContext   - the routing context for analytics (Maybe null when loading user without HTTP call, for instance for test
   * @return a user suitable
   */
  public Future<AuthUser> insertUserFromLoginAuthUserClaims(AuthUser authUserAsClaims, RoutingContext routingContext) {
    User user = toBaseModelUser(authUserAsClaims);
    return this.apiApp
      .getUserProvider()
      .insertUser(user)
      .compose(insertedUser -> {
        AuthUser authUserForSession = this.toAuthUserForSession(insertedUser);
        this.apiApp
          .getApexDomain()
          .getHttpServer()
          .getServer()
          .getTrackerAnalytics()
          .eventBuilder(AnalyticsEventName.SIGN_UP)
          .setUser(authUserForSession)
          .setRoutingContext(routingContext)
          .sendEventAsync();
        return Future.succeededFuture(authUserForSession);
      });

  }

  /**
   * @param user - the user to transform in auth user
   * @return an auth user suitable to be put in a session (ie with role and permission)
   */
  private AuthUser toAuthUserForSession(User user) {

    throw new RuntimeException("todo, add realm ownership" + user);

  }

  public Future<AuthUser> getAuthUserForSessionByClaims(AuthUser authUserClaims) {
    return this.apiApp.getUserProvider()
      .getUserByEmail(authUserClaims.getSubjectEmail(), authUserClaims.getAudience())
      .compose(userInDb -> {
        if (userInDb == null) {
          return Future.succeededFuture();
        }
        return this.apiApp
          .getUserProvider()
          .patchUserIfPropertyValueIsNull(userInDb, toBaseModelUser(authUserClaims))
          .compose(patchUser -> Future.succeededFuture(this.toAuthUserForSession(patchUser)));
      });
  }
}
