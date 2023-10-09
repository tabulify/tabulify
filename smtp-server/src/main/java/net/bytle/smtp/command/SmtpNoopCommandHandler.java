package net.bytle.smtp.command;

import com.sun.mail.smtp.SMTPTransport;
import net.bytle.smtp.*;

/**
 * No operation: NOOP
 * {@link SMTPTransport#isConnected()} sends a NOOP command.
 * to verify the connection status
 */
public class SmtpNoopCommandHandler extends SmtpInputCommandDirectReplyHandler {



  public SmtpNoopCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
    return SmtpReply.createOk();
  }


}
