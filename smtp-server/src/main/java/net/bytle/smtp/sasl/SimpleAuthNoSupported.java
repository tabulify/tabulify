package net.bytle.smtp.sasl;

import net.bytle.smtp.SmtpDomain;
import net.bytle.smtp.SmtpUser;

public class SimpleAuthNoSupported extends SimpleAuth {
  public SimpleAuthNoSupported(SimpleAuthMechanism simpleAuthMechanism) {
    super(simpleAuthMechanism);
  }

  @Override
  public SmtpUser authenticate(SmtpDomain smtpDomain, String credential) throws SimpleAuthException {
    throw new SimpleAuthException("Not supported");
  }

  @Override
  public boolean isImplemented() {
    return false;
  }

}
