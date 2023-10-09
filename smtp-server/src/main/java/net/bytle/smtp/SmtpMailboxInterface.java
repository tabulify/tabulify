package net.bytle.smtp;

public interface SmtpMailboxInterface {


  void deliver(SmtpEnvelope smtpEnvelope) throws SmtpException;

  String getName();


}
