package net.bytle.smtp.mailbox;

import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpException;
import net.bytle.smtp.SmtpUser;

public interface SmtpMailboxInterface {


  void deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage) throws SmtpException;

  String getName();


}
