package net.bytle.smtp;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.mailbox.SmtpMailbox;
import net.bytle.vertx.ConfigAccessor;

import java.util.ArrayList;
import java.util.List;

public class SmtpMailboxMemory extends SmtpMailbox {

  private final List<BMailMimeMessage> messages = new ArrayList<>();

  /**
   * @param vertx          - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  public SmtpMailboxMemory(SmtpUser smtpUser, Vertx vertx, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, configAccessor);
  }

  @Override
  public Future<Void> deliver(BMailMimeMessage mimeMessage) {
    messages.add(mimeMessage);
    return Future.succeededFuture();
  }

  public List<BMailMimeMessage> pumpMessages() {
    List<BMailMimeMessage> actualMessages = new ArrayList<>(this.messages);
    this.messages.clear();
    return actualMessages;
  }

}
