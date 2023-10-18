package net.bytle.tower.eraldy.auth;

import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.type.Strings;

public class UsersUtil {

  /**
   * @param user - the user
   * @return the address email in rfc format as accepted by the email client
   * See <a href="https://vertx.io/docs/vertx-mail-client/java/#_sending_mails">...</a>
   */
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

  public static User vertxUserToEraldyUser(io.vertx.ext.auth.User user) {
    if (user == null) {
      return null;
    }
    return user.principal().mapTo(User.class);
  }

  public static OrganizationUser vertxUserToEraldyOrganizationUser(io.vertx.ext.auth.User user) {
    if (user == null) {
      return null;
    }
    return user.principal().mapTo(OrganizationUser.class);
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

  public static String toHandleRealmIdentifier(User owner) {
    return owner.getHandle() + "@" + owner.getRealm().getHandle() + ".realm";
  }

  public static boolean isEraldyUser(User user) {
    return EraldyDomain.get().isEraldyUser(user);
  }

  public static void assertEraldyUser(User user) {
    EraldyDomain.get().assertIsEraldyUser(user);
  }

}
