package net.bytle.dns;

import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.util.Objects;

public class DnsIp {

  private final InetAddress address;
  private final DnsClient dnsClient;

  public DnsIp(DnsClient session, InetAddress inetAddress) {

    this.dnsClient = session;

    this.address = inetAddress;

  }

  public static DnsIp createFromInetAddress(DnsClient dnsClient, InetAddress inetAddress) {
    return new DnsIp(dnsClient, inetAddress);
  }


  public InetAddress getInetAddress() {
    return this.address;
  }

  @Override
  public String toString() {
    return address.getHostAddress();
  }


  public DnsName getPtrRecord() throws DnsNotFoundException, DnsException {

    return this.dnsClient.resolvePtr(this);


  }

  @SuppressWarnings("unused")
  public int familyOf() {
    return Address.familyOf(this.address);
  }

  public DnsName getReverseDnsName() throws DnsException, DnsNotFoundException {

    return getPtrRecord();

  }

  public String getAddress() {
    return this.address.getHostAddress();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DnsIp dnsIp = (DnsIp) o;
    return Objects.equals(address.getHostAddress(), dnsIp.address.getHostAddress());
  }

  @Override
  public int hashCode() {
    return Objects.hash(address.getHostAddress());
  }


}
