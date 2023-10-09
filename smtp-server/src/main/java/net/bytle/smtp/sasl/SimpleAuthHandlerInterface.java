package net.bytle.smtp.sasl;

import net.bytle.smtp.SmtpDomain;
import net.bytle.smtp.SmtpUser;

public interface SimpleAuthHandlerInterface {

  SmtpUser authenticate(SmtpDomain smtpDomain, String credential) throws SimpleAuthException;

  boolean isImplemented();

}
