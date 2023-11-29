package net.bytle.dns;

import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class DnsClientAbs implements DnsClient{

  public DnsName createDnsName(String name) throws DnsIllegalArgumentException {
    return new DnsName(this, name);
  }

  @SuppressWarnings("unused")
  public DnsIp createFromIpString(String ipAddress) throws UnknownHostException {
    InetAddress inetAddress = Address.getByAddress(ipAddress);
    return new DnsIp(this, inetAddress);
  }

  public DnsIp createIpFromAddress(InetAddress inetAddress) {
    return DnsIp.createFromInetAddress(this, inetAddress);
  }

}
