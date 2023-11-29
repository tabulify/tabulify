package net.bytle.dns;

import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class DnsIp {

  private final InetAddress address;

  public DnsIp(InetAddress inetAddress) {

    this.address = inetAddress;

  }

  public static DnsIp createFromInetAddress(InetAddress inetAddress) {
    return new DnsIp(inetAddress);
  }

  @SuppressWarnings("unused")
  public static DnsIp createFromString(String ipAddress) throws DnsException {
    InetAddress inetAddress;
    try {
      inetAddress = Address.getByAddress(ipAddress);
    } catch (UnknownHostException e) {
      throw new DnsException("Invalid Address",e);
    }
    return createFromInetAddress(inetAddress);
  }

  public static DnsIp createFromIpv4String(String ipv4) throws DnsException {
    InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByAddress(Address.toByteArray(ipv4, Address.IPv4));
    } catch (UnknownHostException e) {
      throw new DnsException("Bad Ip v4 ("+ipv4+")",e);
    }
    return new DnsIp(inetAddress);
  }

  public static DnsIp createFromIpv6String(String ipv6) throws DnsException {
    InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByAddress(Address.toByteArray(ipv6, Address.IPv6));
    } catch (UnknownHostException e) {
      throw new DnsException("Bad Ip v6 ("+ipv6+")",e);
    }
    return new DnsIp(inetAddress);
  }


  public InetAddress getInetAddress() {
    return this.address;
  }

  @Override
  public String toString() {
    return address.getHostAddress();
  }


  @SuppressWarnings("unused")
  public int familyOf() {
    return Address.familyOf(this.address);
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
