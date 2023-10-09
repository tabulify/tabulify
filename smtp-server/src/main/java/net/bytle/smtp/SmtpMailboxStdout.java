package net.bytle.smtp;

public class SmtpMailboxStdout extends SmtpMailbox {


  @Override
  public void deliver(SmtpEnvelope smtpEnvelope) {

    System.out.println("Delivery on Stdout:");
    System.out.println(smtpEnvelope.getMimeMessage().toEml());

  }

  public String getName() {
    return "stdout";
  }

}
