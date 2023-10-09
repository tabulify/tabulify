package net.bytle.smtp;

public class SmtpMailboxForward extends SmtpMailbox{

  /**
   * Forward must be implemented with SRS
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.2">Note on Forward</a>
   */
  @Override
  public void deliver(SmtpEnvelope smtpEnvelope) throws SmtpException {
    throw SmtpException.createNotSupportedImplemented("Forwarding is not yet supported");
  }

  @Override
  public String getName() {
    return "forward";
  }

}
