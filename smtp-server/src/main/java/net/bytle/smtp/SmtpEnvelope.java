package net.bytle.smtp;

import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailMimeMessage;

import java.util.Set;

/**
 * An envelope is a sender, a recipient and the message
 */
public class SmtpEnvelope {


  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final BMailInternetAddress sender;
  private final Set<SmtpRecipient> recipients;
  private final BMailMimeMessage mimeMessage;

  public SmtpEnvelope(BMailInternetAddress sender, Set<SmtpRecipient> recipients, BMailMimeMessage mimeMessage) {
    this.sender = sender;
    this.recipients = recipients;
    this.mimeMessage = mimeMessage;
  }

  public static SmtpEnvelope create(BMailInternetAddress sender, Set<SmtpRecipient> recipients, BMailMimeMessage mimeMessage) {
    return new SmtpEnvelope(sender, recipients,mimeMessage);
  }

  public Set<SmtpRecipient> getRecipients() {
    return this.recipients;
  }

  public BMailMimeMessage getMimeMessage() {
    return this.mimeMessage;
  }

}
