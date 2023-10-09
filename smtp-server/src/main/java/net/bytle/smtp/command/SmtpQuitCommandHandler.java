package net.bytle.smtp.command;

import net.bytle.smtp.*;

public class SmtpQuitCommandHandler extends SmtpInputCommandDirectReplyHandler {


  public SmtpQuitCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  public SmtpReply getReply(SmtpInputContext smtpInputContext) {

    String domainOrHostname = smtpInputContext.getSession().getGreeting().getDomainOrHostName();
    return SmtpReply.create(SmtpReplyCode.CLOSING_QUITING_221, domainOrHostname + " Service closing transmission channel");

  }


}
