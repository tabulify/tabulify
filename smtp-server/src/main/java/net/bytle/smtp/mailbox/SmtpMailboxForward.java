package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.smtp.SmtpMessage;
import net.bytle.smtp.SmtpUser;
import net.bytle.smtp.milter.SmtpMilter;
import net.bytle.vertx.ConfigAccessor;

import java.util.List;

public class SmtpMailboxForward extends SmtpMailbox {

  public SmtpMailboxForward(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, milters, configAccessor);
  }

  /**
   * Forward must be implemented with SRS
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.2">Note on Forward</a>
   */
  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {
    return Future.failedFuture("Forwarding is not yet supported");
  }


}
