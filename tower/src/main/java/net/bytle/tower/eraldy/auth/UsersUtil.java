package net.bytle.tower.eraldy.auth;

import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.type.Strings;
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
   * @param firstName         - the default first name
   * @param possibleFirstName - the other first name candidate
   * @return the first name based on the case. If otherName is lowercase and defaultName is uppercase, otherName is returned)
   */
  public static String getFirstNameFromCase(String firstName, String possibleFirstName) {
    if (possibleFirstName == null) {
      return firstName;
    }
    if (Strings.isUpperCase(firstName) && !Strings.isUpperCase(possibleFirstName)) {
      return possibleFirstName;
    }
    return firstName;
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


}
