package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpUser;
import net.bytle.vertx.ConfigAccessor;

public class SmtpMailboxForward extends SmtpMailbox{

  public SmtpMailboxForward(Vertx vertx, ConfigAccessor configAccessor) {
    super(vertx, configAccessor);
  }

  /**
   * Forward must be implemented with SRS
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.2">Note on Forward</a>
   */
  @Override
  public Future<Void> deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage) {
    return Future.failedFuture("Forwarding is not yet supported");
  }

  @Override
  public String getName() {
    return "forward";
  }

}
