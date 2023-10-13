package net.bytle.dns;

import org.xbill.DNS.Address;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsSession {

  private final LookupSession lookupSession;
  private final Resolver dnsResolver;

  public DnsSession(DnsSessionBuilder dnsSessionBuilder) {
    dnsResolver = dnsSessionBuilder.resolver;
    lookupSession = LookupSession
      .builder()
      .resolver(dnsResolver)
      .build();
  }


  public static DnsSessionBuilder builder() {
    return new DnsSessionBuilder();
  }

  public static DnsSession createDefault() {
    return DnsSession.builder().build();
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

  public DnsException handleLookupException(DnsName dnsName, Exception e) throws DnsNotFoundException {
    if (e.getCause() instanceof NoSuchDomainException) {
      throw new DnsNotFoundException("The domain name does not exist (" + dnsName + ")",e);
    }
    return new DnsException(e);
  }

  public DnsHost.DnsHostConfig configHost(String name) throws DnsIllegalArgumentException {
    return new DnsHost.DnsHostConfig(this, name);
  }


  public static class DnsSessionBuilder  {

    private Resolver resolver = DnsResolver.getLocal();

    public DnsSessionBuilder setResolverToCloudflare() {
      this.resolver = DnsResolver.getCloudflare();
      return this;
    }

    public DnsSession build() {
      return new DnsSession(this);
    }
  }
}
