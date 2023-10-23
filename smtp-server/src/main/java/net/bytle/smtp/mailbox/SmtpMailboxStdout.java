package net.bytle.smtp.mailbox;

import net.bytle.email.BMailMimeMessage;
import net.bytle.smtp.SmtpUser;

public class SmtpMailboxStdout extends SmtpMailbox {


  @Override
  public void deliver(SmtpUser smtpUser, BMailMimeMessage mimeMessage) {

    System.out.println("Delivery on Stdout to " + smtpUser + ":");
    System.out.println(mimeMessage.toEml());

  }

  public String getName() {
    return "stdout";
  }

}
