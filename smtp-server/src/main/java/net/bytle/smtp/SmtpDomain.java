package net.bytle.smtp;

import net.bytle.dns.DnsIllegalArgumentException;
import net.bytle.dns.DnsName;
import net.bytle.dns.XBillDnsClient;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A class that wraps a domain name
 * for the SMTP context
 */
public class SmtpDomain {


  private final DnsName domainName;
  private final Map<String, SmtpUser> users = new HashMap<>();

  public SmtpDomain(String name) throws DnsIllegalArgumentException {

    this.domainName = XBillDnsClient.createDefault().createDnsName(name);

  }

  public static SmtpDomain createFromName(String name) throws DnsIllegalArgumentException {
    return new SmtpDomain(name);
  }


  public DnsName getDnsDomain() {
    return domainName;
  }

  public SmtpUser getUser(String userName) throws NotFoundException {
    SmtpUser user = this.users.get(userName);
    if (user == null) {
      throw new NotFoundException();
    }
    return user;
  }

  @Override
  public String toString() {
    return getDnsDomain().toStringWithoutRoot();
  }

  public SmtpDomain addUser(SmtpUser smtpUser) {
    if (!smtpUser.getDomain().equals(this)) {
      throw new InternalException("The  domain (" + this + ") does not accept users of another domain (" + smtpUser + ")");
    }
    this.users.put(smtpUser.getName(), smtpUser);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SmtpDomain that = (SmtpDomain) o;
    return Objects.equals(domainName, that.domainName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domainName.toString());
  }

}
