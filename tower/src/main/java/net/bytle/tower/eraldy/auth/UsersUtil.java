package net.bytle.tower.eraldy.auth;

import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.auth.AuthUserUtils;
import net.bytle.vertx.flow.SmtpSender;

public class UsersUtil {

  /**
   * @param user - the user
   * @return the address email in rfc format as accepted by the email client
   * See <a href="https://vertx.io/docs/vertx-mail-client/java/#_sending_mails">...</a>
   * @deprecated use {@link BMailInternetAddress} instead
   */
  @Deprecated
  public static String getEmailAddressWithName(User user) {
    String address = user.getEmailAddress().toNormalizedString();
    if (address == null) {
      throw new InternalException("The user email should not be null");
    }
    String name = user.getGivenName();
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

    return AuthUserUtils.getNameOrNameFromEmail(user.getGivenName(), user.getEmailAddress());

  }


  public static SmtpSender toSenderUser(User user) {
    SmtpSender smtpSender = new SmtpSender();
    try {
      smtpSender.setName(UsersUtil.getNameOrNameFromEmail(user));
    } catch (NotFoundException | AddressException e) {
      throw new InternalException(e);
    }
    smtpSender.setEmail(user.getEmailAddress());
    smtpSender.setFullName(user.getFamilyName());
    smtpSender.setAvatar(user.getAvatar());
    smtpSender.setTitle(user.getTitle());
    return smtpSender;
  }


}
