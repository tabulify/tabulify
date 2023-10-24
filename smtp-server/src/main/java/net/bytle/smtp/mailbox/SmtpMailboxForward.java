package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpUser;
import net.bytle.vertx.ConfigAccessor;

public class SmtpMailboxForward extends SmtpMailbox {

  public SmtpMailboxForward(SmtpUser smtpUser, Vertx vertx, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, configAccessor);
  }

  /**
   * Forward must be implemented with SRS
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.2">Note on Forward</a>
   */
  @Override
  public Future<Void> deliver(BMailMimeMessage mimeMessage) {
    return Future.failedFuture("Forwarding is not yet supported");
  }


}
