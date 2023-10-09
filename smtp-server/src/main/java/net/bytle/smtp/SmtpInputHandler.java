package net.bytle.smtp;

/**
 *
 */
public interface SmtpInputHandler {



  void handle(SmtpInputContext smtpInputContext) throws SmtpException;


}
