package net.bytle.smtp;

import io.vertx.core.Future;
import net.bytle.smtp.mailbox.SmtpMailbox;

/**
 * A user
 */
public class SmtpUser {


  private final String name;
  private final SmtpDomain smtpDomain;
  private SmtpMailbox mailBox;
  private String password;

  public SmtpUser(SmtpDomain smtpDomain, String name) {
    this.smtpDomain = smtpDomain;
    this.name = name.trim().toLowerCase();
  }

  public static SmtpUser createFrom(SmtpDomain smtpDomain, String userName, SmtpMailbox smtpMailbox, String password) {

    SmtpUser smtpUser = new SmtpUser(smtpDomain, userName);
    smtpUser.mailBox = smtpMailbox;
    smtpUser.password = password;
    return smtpUser;

  }


  public SmtpDomain getDomain() {
    return this.smtpDomain;
  }

  public String getName() {
    return this.name;
  }

  public String getPassword() {
    return this.password;
  }

  public Future<Void> deliver(SmtpDeliveryEnvelope smtpDeliveryEnvelope) {
    return this.mailBox.deliver(this, smtpDeliveryEnvelope.getMimeMessage());
  }

  @Override
  public String toString() {
    return name + "@" + this.smtpDomain;
  }
}
