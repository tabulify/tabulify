package net.bytle.tower.eraldy.auth;

import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.EraldyDomain;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.SmtpSender;

public class UsersUtil {

  /**
   * @deprecated use {@link BMailInternetAddress} instead
   * @param user - the user
   * @return the address email in rfc format as accepted by the email client
   * See <a href="https://vertx.io/docs/vertx-mail-client/java/#_sending_mails">...</a>
   */
  @Deprecated
  public static String getEmailAddressWithName(User user) {
    String address = user.getEmail();
    if (address == null) {
      throw new InternalException("The user email should not be null");
    }
    String name = user.getName();
    if (name != null) {
      /**
       * We don't use the format `email (name)`
       * because the email library returns an error when the email has a date suffix
       * ie `joe-20221022-1010@doe.com (Joe)` is not value
       */
      address = name + " <" + address + ">";
    }
    return address;
  }

  /**
   * @param user - a user
   * @return the name or the email local part as name
   * @throws NotFoundException - no name or email
   * @throws AddressException  - bad email address
   */
  public static String getNameOrNameFromEmail(User user) throws NotFoundException, AddressException {
    String name = user.getName();
    if (name != null) {
      return name;
    }
    String email = user.getEmail();
    if (email == null) {
      // should not happen as an email is mandatory
      throw new NotFoundException("No name could be found for this user");
    }
    return BMailInternetAddress.of(email)
      .getLocalPart();

  }



  /**
   * @param user - the user
   * @return a user with default value that can be used for templating
   */
  public static User getPublicUserForTemplateWithDefaultValues(User user) {
    User outputUser = new User();
    outputUser.setEmail(user.getEmail());
    String defaultName;
    try {
      defaultName = UsersUtil.getNameOrNameFromEmail(user);
      outputUser.setName(defaultName);
    } catch (NotFoundException | AddressException e) {
      throw new InternalException("Should not occurs, a database user should have a valid email at least", e);
    }
    outputUser.setFullname(user.getFullname() != null ? user.getFullname() : defaultName);
    outputUser.setAvatar(user.getAvatar());
    outputUser.setTitle(user.getTitle());
    return outputUser;
  }

  public static boolean isEraldyUser(User user) {
    return EraldyDomain.get().isEraldyId(user.getRealm().getLocalId());
  }

  public static void assertEraldyUser(User user) {
    EraldyDomain.get().assertIsEraldyUser(user.getRealm().getLocalId());
  }


  public static AuthUser toAuthUserClaims(User appUser) {
    AuthUser authUserClaims = new AuthUser();
    authUserClaims.setSubject(appUser.getGuid());
    authUserClaims.setSubjectHandle(appUser.getHandle());
    authUserClaims.setSubjectEmail(appUser.getEmail());
    authUserClaims.setAudience(appUser.getRealm().getGuid());
    authUserClaims.setAudienceHandle(appUser.getRealm().getHandle());
    return authUserClaims;
  }


  public static SmtpSender toSenderUser(User user) {
    SmtpSender smtpSender = new SmtpSender();
    try {
      smtpSender.setName(UsersUtil.getNameOrNameFromEmail(user) );
    } catch (NotFoundException | AddressException e) {
      throw new InternalException(e);
    }
    smtpSender.setEmail(user.getEmail());
    smtpSender.setFullName(user.getFullname());
    smtpSender.setAvatar(user.getAvatar());
    smtpSender.setTitle(user.getTitle());
    return smtpSender;
  }


  public static User toEraldyUser(AuthUser authUser, EraldyApiApp eraldyApiApp) {
    Realm realm = new Realm();
    realm.setGuid(authUser.getAudience());
    try {
      Guid realmGuid = eraldyApiApp.getRealmProvider().getGuidFromHash(authUser.getAudience());
      realm.setLocalId(realmGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
    User userEraldy = new User();
    userEraldy.setRealm(realm);
    userEraldy.setName(authUser.getSubjectGivenName());
    userEraldy.setEmail(authUser.getSubjectEmail());
    try {
      String subject = authUser.getSubject();
      userEraldy.setGuid(subject);
      Guid guid = eraldyApiApp.getUserProvider().getGuid(subject);
      userEraldy.setLocalId(guid.validateRealmAndGetFirstObjectId(realm.getLocalId()));
    } catch (CastException e) {
      throw new RuntimeException(e);
    }

    return userEraldy;

    /**
     * For OpenID Connect/OAuth2 Access Tokens,
     * there is a rootClaim
     */
//    String rootClaim = user.attributes().getString("rootClaim");
//    if (rootClaim != null && rootClaim.equals("accessToken")) {
//      // JWT
//      String userGuid = user.principal().getString("sub");
//      if (userGuid == null) {
//        return Future.failedFuture(ValidationException.create("The sub is empty", "sub", null));
//      }
//      if(clazz.equals(OrganizationUser.class)) {
//        //noinspection unchecked
//        return (Future<T>) this
//          .getOrganizationUserProvider()
//          .getOrganizationUserByGuid(userGuid);
//      }
//    }
  }
}
