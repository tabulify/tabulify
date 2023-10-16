package net.bytle.smtp.mailbox;

import net.bytle.smtp.SmtpEnvelope;
import net.bytle.smtp.SmtpException;

public interface SmtpMailboxInterface {


  void deliver(SmtpEnvelope smtpEnvelope) throws SmtpException;

  String getName();


}
