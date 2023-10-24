package net.bytle.smtp.mailbox;

import io.vertx.core.Vertx;
import net.bytle.smtp.SmtpUser;
import net.bytle.smtp.milter.SmtpMilter;
import net.bytle.vertx.ConfigAccessor;

import java.util.List;

public abstract class SmtpMailbox implements SmtpMailboxInterface {

  private final SmtpUser smtpUser;
  private final Vertx vertx;
  private final List<SmtpMilter> milters;

  /**
   * @param vertx          - the vertx in case an async should be run on the event bus
   * @param configAccessor - the configuration
   * @param milters - the milters to apply before reception
   */
  public SmtpMailbox(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, @SuppressWarnings("unused") ConfigAccessor configAccessor) {
    this.smtpUser = smtpUser;
    this.vertx = vertx;
    this.milters = milters;
  }

  public SmtpUser getSmtpUser() {
    return smtpUser;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public List<SmtpMilter> getMilters() {
    return milters;
  }
}
