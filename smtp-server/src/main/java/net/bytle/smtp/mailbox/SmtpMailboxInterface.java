package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import net.bytle.smtp.SmtpMessage;

public interface SmtpMailboxInterface {


  Future<Void> deliver(SmtpMessage smtpMessage);


}
