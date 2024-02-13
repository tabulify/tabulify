package net.bytle.dns;

import java.util.Objects;

/**
 * A basic DNS email address to
 * handle Email validation in DNS records.
 * <p>
 * The email address object {@link net.bytle.email.BMailInternetAddress} depends now of a lot of package
 * and also from this package.
 * Therefore, it's difficult to not create a circular dependencies between DNS and the Smtp Package. ie
 * * There is an email in dmarc, we need it then in DNS to verify that this an email.
 * * And if the smtp package does need it also.
 */
public class DnsEmailAddress {


  private final String mailAddress;
  private final DnsName domain;
  private final String localPart;

  public DnsEmailAddress(String mailAddress) throws DnsException {
    this.mailAddress = mailAddress;
    final String[] split = mailAddress.split("@");
    if (split.length != 2) {
      throw new DnsException("The email address does not have 2 parts (ie localname@domain)");
    }
    String absoluteName = split[1];
    try {
      this.domain = DnsName.create(absoluteName);
    } catch (DnsIllegalArgumentException e) {
      throw new DnsException("The domain (" + absoluteName + ")is not valid (");
    }
    this.localPart = split[0];
  }

  public static DnsEmailAddress of(String mail) throws DnsException {
    return new DnsEmailAddress(mail);
  }

  @Override
  public String toString() {
    return mailAddress;
  }

  public DnsName getDomainName() {
    return this.domain;
  }

  @SuppressWarnings("unused")
  public String getLocalPart() {
    return localPart;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DnsEmailAddress that = (DnsEmailAddress) o;
    return Objects.equals(mailAddress, that.mailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mailAddress);
  }
}
