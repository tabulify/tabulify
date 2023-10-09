package net.bytle.smtp;

import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.NullValueException;

/**
 * The envelope recipient
 * <p>
 * Note on the diff between `recipients` from the rcpt command and `to` from the message
 * <p>
 * The message may be sent to:
 * * multiple recipient that are not in our domain
 * * a list
 * Therefore the recipient from the RCP command and the `to` address from the message
 * may not be the same at all
 * Example for Google Group:
 * To: Abridged recipients <vertx@googlegroups.com>
 * From: vertx@googlegroups.com
 */
public class SmtpRecipient {
  private final SmtpDeliveryType delivery;
  private final BMailInternetAddress internetAddres;
  private final SmtpUser smtpUser;

  private SmtpRecipient(BMailInternetAddress internetAddress, SmtpDeliveryType delivery, SmtpUser smtpUser) {
    this.internetAddres = internetAddress;
    this.delivery = delivery;
    this.smtpUser = smtpUser;
  }

  public static SmtpRecipient createFrom(SmtpSession smtpSession, String forwardPathString) throws SmtpException {

    /**
     * Forward path to internet address
     */
    BMailInternetAddress recipientInternetAddress;
    try {
      SmtpPostMaster postmaster = smtpSession.getGreeting().getRequestedHostOrDefault().getPostmaster();
      recipientInternetAddress = SmtpPath.of(forwardPathString).getInternetAddress(postmaster);
    } catch (SmtpException | NullValueException e) {
      throw  SmtpException.create(SmtpReplyCode.USER_AMBIGUOUS_553, forwardPathString + " Invalid TO forward path address.");
    }

    /**
     * Check and processing
     * Determine the transaction delivery
     */
    BMailInternetAddress mailFromSender = smtpSession.getTransactionState().getSender();
    SmtpDeliveryType delivery;
    if (smtpSession.isFirstPartyEmail(mailFromSender)) {
      SmtpFiltering.checkFirstPartySenderShouldBeAuthenticated(smtpSession, mailFromSender);
      if (smtpSession.isFirstPartyEmail(recipientInternetAddress)) {
        delivery = SmtpDeliveryType.LOCAL;
      } else {
        delivery = SmtpDeliveryType.REMOTE;
      }
    } else {
      if (smtpSession.isFirstPartyEmail(recipientInternetAddress)) {
        delivery = SmtpDeliveryType.LOCAL;
      } else {
        throw SmtpException.create(SmtpReplyCode.NO_SUCH_USER_550, "User is unknown (not a local user)")
          .setBadBehaviorFlag();
      }
    }

    /**
     * Do we have the mailboxes
     */
    SmtpUser smtpUser = null;
    if (delivery == SmtpDeliveryType.LOCAL) {
      smtpUser = smtpSession.checkUserExists(recipientInternetAddress);
    }

    return new SmtpRecipient(recipientInternetAddress, delivery, smtpUser);
  }

  @Override
  public String toString() {
    return internetAddres.toString();
  }

  public SmtpDeliveryType getDeliveryType() {
    return this.delivery;
  }

  public SmtpUser getLocalUser() {
    return this.smtpUser;
  }
}
