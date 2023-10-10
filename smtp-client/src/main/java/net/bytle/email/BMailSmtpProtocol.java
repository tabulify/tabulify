package net.bytle.email;

public enum BMailSmtpProtocol {

  /**
   * You got a {@link com.sun.mail.smtp.SMTPTransport}
   */
  SMTP("smtp"),
  /**
   * You got a {@link com.sun.mail.smtp.SMTPSSLTransport}
   */
  SMTPS("smtps");

  private final String protocolName;

  BMailSmtpProtocol(String protocolName) {
    this.protocolName = protocolName;
  }

  @Override
  public String toString() {
    return protocolName;
  }

}
