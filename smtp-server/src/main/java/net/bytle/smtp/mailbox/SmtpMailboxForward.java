package net.bytle.smtp.mailbox;

import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpException;
import net.bytle.smtp.SmtpUser;

public class SmtpMailboxForward extends SmtpMailbox{

  /**
   * Forward must be implemented with SRS
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.2">Note on Forward</a>
   */
  @Override
  public void deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage) throws SmtpException {
    throw SmtpException.createNotSupportedImplemented("Forwarding is not yet supported");
  }

  @Override
  public String getName() {
    return "forward";
  }

}
