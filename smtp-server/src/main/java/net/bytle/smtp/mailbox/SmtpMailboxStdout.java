package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.smtp.SmtpMessage;
import net.bytle.smtp.SmtpUser;
import net.bytle.smtp.milter.SmtpMilter;
import net.bytle.vertx.ConfigAccessor;

import java.util.List;

public class SmtpMailboxStdout extends SmtpMailbox {


  /**
   * @param vertx          - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  public SmtpMailboxStdout(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, milters, configAccessor);
  }

  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {

    System.out.println("Delivery on Stdout to " + this.getSmtpUser() + ":");
    System.out.println(new String(smtpMessage.getBytes()));
    return Future.succeededFuture();

  }


}
