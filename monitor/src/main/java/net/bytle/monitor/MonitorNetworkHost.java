package net.bytle.monitor;

import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MonitorNetworkHost {
  private final MonitorNetworkHostConfig config;

  public MonitorNetworkHost(MonitorNetworkHostConfig config) {
    this.config = config;
  }

  public static MonitorNetworkHostConfig createForName(String name) {
    return new MonitorNetworkHostConfig(name);
  }

  public String getName() {
    return this.config.name;
  }

  public InetAddress getIpv4() {
    return this.config.ipv4;
  }

  public InetAddress getIpv6() {
    return this.config.ipv6;
  }


  public static class MonitorNetworkHostConfig  {
    private final String name;
    private InetAddress ipv4;
    private InetAddress ipv6;

    public MonitorNetworkHostConfig(String name) {
      this.name = name;
    }
    public MonitorNetworkHostConfig setIpv4(String ipv4) throws UnknownHostException {


      this.ipv4 = InetAddress.getByAddress(this.name, Address.toByteArray(ipv4,Address.IPv4));
      return this;
    }

    public MonitorNetworkHostConfig setIpv6(String ipv6) throws UnknownHostException {
      this.ipv6 = InetAddress.getByAddress(this.name, Address.toByteArray(ipv6,Address.IPv6));
      return this;
    }

    public MonitorNetworkHost build() {
      return new MonitorNetworkHost(this);
    }
  }

  @Override
  public String toString() {
    return config.name;
  }

}
