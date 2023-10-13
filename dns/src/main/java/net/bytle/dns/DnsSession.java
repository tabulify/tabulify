package net.bytle.dns;

import org.xbill.DNS.Address;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.lookup.LookupSession;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsSession {

  private final LookupSession lookupSession;
  private final Resolver dnsResolver;

  public DnsSession() {
    dnsResolver = DnsResolver.getLocal();
    lookupSession = LookupSession
      .builder()
      .resolver(dnsResolver)
      .build();
  }

  public static DnsSession build() {
    return new DnsSession();
  }

  public DnsName createDnsName(String name) throws DnsIllegalArgumentException {
    return new DnsName(this, name);
  }
  @SuppressWarnings("unused")
  public DnsIp createFromIpString(String ipAddress) throws UnknownHostException {
    InetAddress inetAddress = Address.getByAddress(ipAddress);
    return new DnsIp(this,inetAddress);
  }

  /**
   *
   * @deprecated use {@link #getLookupSession()} instead
   */
  @Deprecated
  public Resolver getDnsResolver() {
    return dnsResolver;
  }

  public LookupSession getLookupSession() {
    return this.lookupSession;
  }

  public DnsIp createIpFromAddress(InetAddress inetAddress) {
    return DnsIp.createFromInetAddress(this, inetAddress);
  }
}
