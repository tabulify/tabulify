package net.bytle.smtp.mailbox;

import io.vertx.core.Future;
import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpUser;

public interface SmtpMailboxInterface {


  Future<Void> deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage);

  String getName();


}
