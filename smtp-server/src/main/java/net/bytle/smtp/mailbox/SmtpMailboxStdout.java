package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpUser;
import net.bytle.vertx.ConfigAccessor;

public class SmtpMailboxStdout extends SmtpMailbox {


  /**
   * @param vertx          - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  public SmtpMailboxStdout(Vertx vertx, ConfigAccessor configAccessor) {
    super(vertx, configAccessor);
  }

  @Override
  public Future<Void> deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage) {

    System.out.println("Delivery on Stdout to " + smtpUser + ":");
    System.out.println(mimeMessage.toEml());
    return Future.succeededFuture();

  }

  public String getName() {
    return "stdout";
  }

}
