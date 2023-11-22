package net.bytle.tower.eraldy.objectProvider;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.AssertionException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.exception.NotSignedInOrganizationUser;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.auth.AuthUser;

import java.lang.reflect.InvocationTargetException;

public class AuthUserProvider {
  private final EraldyApiApp apiApp;

  public AuthUserProvider(EraldyApiApp eraldyApiApp) {
    this.apiApp = eraldyApiApp;
  }

  public OrganizationUser getSignedInOrganizationalUser(RoutingContext routingContext) throws NotFoundException, NotSignedInOrganizationUser {
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


  public User getSignedInBaseUser(RoutingContext routingContext) throws NotFoundException {
    AuthUser authUser = this.getSignedInAuthUser(routingContext);
    try {
      return toModelUser(authUser, User.class);
    } catch (NotSignedInOrganizationUser e) {
      // the exception is for an organizational user
      // it should not happen as we ask a model user
      throw new InternalException("The auth user could ne be retrieved as base user", e);
    }
  }

}
