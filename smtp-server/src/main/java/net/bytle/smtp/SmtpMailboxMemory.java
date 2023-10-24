package net.bytle.smtp;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bytle.smtp.mailbox.SmtpMailbox;
import net.bytle.smtp.milter.SmtpMilter;
import net.bytle.vertx.ConfigAccessor;

import java.util.ArrayList;
import java.util.List;

public class SmtpMailboxMemory extends SmtpMailbox {

  private final List<SmtpMessage> messages = new ArrayList<>();

  /**
   * @param vertx          - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  public SmtpMailboxMemory(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, milters, configAccessor);
  }

  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {
    messages.add(smtpMessage);
    return Future.succeededFuture();
  }

  public List<SmtpMessage> pumpMessages() {
    List<SmtpMessage> actualMessages = new ArrayList<>(this.messages);
    this.messages.clear();
    return actualMessages;
  }

}
