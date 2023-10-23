package net.bytle.smtp.mailbox;

import io.vertx.core.Vertx;
import net.bytle.vertx.ConfigAccessor;

public abstract class SmtpMailbox implements SmtpMailboxInterface {

  /**
   *
   * @param vertx - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  @SuppressWarnings("unused")
  public SmtpMailbox(Vertx vertx, ConfigAccessor configAccessor) {
  }

}
