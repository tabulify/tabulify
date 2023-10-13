package net.bytle.dns;

import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsHost {
  private final DnsHostConfig config;

  public DnsHost(DnsHostConfig config) {
    this.config = config;
  }



  public String getName() {
    return this.config.name;
  }

  public DnsIp getIpv4() {
    return this.config.ipv4;
  }

  public DnsIp getIpv6() {
    return this.config.ipv6;
  }


  public static class DnsHostConfig {
    private final String name;
    private final DnsSession dnsSession;
    private DnsIp ipv4;
    private DnsIp ipv6;

    public DnsHostConfig(DnsSession dnsSession, String name) {

      this.dnsSession = dnsSession;
      this.name = name;

    }

    public DnsHostConfig setIpv4(String ipv4) throws UnknownHostException {
      InetAddress inetAddress = InetAddress.getByAddress(this.name, Address.toByteArray(ipv4, Address.IPv4));
      this.ipv4 = dnsSession.createIpFromAddress(inetAddress);
      return this;
    }

    public DnsHostConfig setIpv6(String ipv6) throws UnknownHostException {
      InetAddress inetAddress = InetAddress.getByAddress(this.name, Address.toByteArray(ipv6, Address.IPv6));
      this.ipv6 = dnsSession.createIpFromAddress(inetAddress);
      return this;
    }

    public DnsHost build() {
      return new DnsHost(this);
    }
  }

  @Override
  public String toString() {
    return config.name;
  }

}
