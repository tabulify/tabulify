package net.bytle.smtp.mailbox;

import io.vertx.core.Vertx;
import net.bytle.smtp.SmtpUser;
import net.bytle.vertx.ConfigAccessor;

public abstract class SmtpMailbox implements SmtpMailboxInterface {

  private final SmtpUser smtpUser;
  private final Vertx vertx;

  /**
   *
   * @param vertx - the vertx in case an async should be run on the event bus
   * @param configAccessor - the configuration
   */
  public SmtpMailbox(SmtpUser smtpUser, Vertx vertx, @SuppressWarnings("unused") ConfigAccessor configAccessor) {
    this.smtpUser = smtpUser;
    this.vertx = vertx;
  }

  public SmtpUser getSmtpUser() {
    return smtpUser;
  }

  public Vertx getVertx() {
    return vertx;
  }
}
