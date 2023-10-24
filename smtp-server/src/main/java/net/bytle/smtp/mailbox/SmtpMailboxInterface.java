package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import net.bytle.email.BMailMimeMessage;

public interface SmtpMailboxInterface {


  Future<Void> deliver(BMailMimeMessage mimeMessage);



}
