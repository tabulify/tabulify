package net.bytle.dns;

import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class XBillDnsClient extends DnsClientAbs {

  private final LookupSession lookupSession;

  public XBillDnsClient(DnsClientBuilder dnsClientBuilder) {
    Resolver dnsResolver = dnsClientBuilder.resolver;
    lookupSession = LookupSession
      .builder()
      .resolver(dnsResolver)
      .build();
  }


  public static DnsClientBuilder builder() {
    return new DnsClientBuilder();
  }

  public static XBillDnsClient createDefault() {
    return XBillDnsClient.builder().build();
  }

  private static String getStringFromTxtRecord(TXTRecord txtRecord) {
    return String.join("", txtRecord.getStrings());
  }


  @Override
  public Set<DnsIp> resolveA(DnsName dnsName) throws DnsNotFoundException, DnsException {
    try {
      return this
        .getLookupSession().lookupAsync(getXbillName(dnsName), Type.A)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream().map(ARecord.class::cast)
        .map(ARecord::getAddress)
        .map(this::createIpFromAddress)
        .collect(Collectors.toSet());
    } catch (Exception e) {
      throw this.handleLookupException(dnsName, e);
    }
  }

  @Override
  public Set<DnsName> lookupCName(DnsName dnsName) throws DnsNotFoundException, DnsException {
    try {
      return this.getLookupSession()
        .lookupAsync(getXbillName(dnsName), Type.CNAME)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(CNAMERecord.class::cast)
        .map(rec-> {
          try {
            return this.createDnsName(rec.getTarget().toString());
          } catch (DnsIllegalArgumentException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toSet());
    } catch (Exception e) {
      throw this.handleLookupException(dnsName, e);
    }
  }

  @Override
  public DnsName resolvePtr(DnsIp dnsIp) throws DnsNotFoundException, DnsException {
    Name name = ReverseMap.fromAddress(dnsIp.getInetAddress());
    try {
      return this
        .getLookupSession()
        .lookupAsync(name, Type.PTR)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(PTRRecord.class::cast)
        .map(rec-> {
          try {
            return this.createDnsName(rec.getTarget().toString());
          } catch (DnsIllegalArgumentException e) {
            throw new RuntimeException(e);
          }
        })
        .findFirst()
        .orElseThrow(DnsNotFoundException::new);
    } catch (Exception e) {
      DnsName dnsName;
      String reverseName = name.toString();
      try {
        dnsName = this.createDnsName(reverseName);
      } catch (DnsIllegalArgumentException ex) {
        throw new DnsInternalException("The reverse name should be good ("+reverseName+")");
      }
      throw this.handleLookupException(dnsName, e);
    }
  }


  private LookupSession getLookupSession() {
    return this.lookupSession;
  }


  public DnsException handleLookupException(DnsName dnsName, Exception e) throws DnsNotFoundException {
    if (e.getCause() instanceof NoSuchDomainException) {
      throw new DnsNotFoundException("The domain name does not exist (" + dnsName + ")", e);
    }
    return new DnsException(e);
  }

  public DnsHost.DnsHostConfig configHost(String name) throws DnsIllegalArgumentException {
    return new DnsHost.DnsHostConfig(this, name);
  }

  @Override
  public List<String> resolveTxt(DnsName dnsName) throws DnsException, DnsNotFoundException {

    try {
      return this
        .getLookupSession()
        .lookupAsync(getXbillName(dnsName), Type.TXT)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(TXTRecord.class::cast)
        .map(XBillDnsClient::getStringFromTxtRecord)
        .collect(Collectors.toList());
    } catch (Exception e) {
      throw this.handleLookupException(dnsName, e);
    }
  }

  private Name getXbillName(DnsName dnsName) throws DnsException {
    try {
      return Name.fromString(dnsName.getAbsoluteName());
    } catch (TextParseException e) {
      throw new DnsException(e);
    }
  }

  @Override
  public List<DnsMxRecord> resolveMx(DnsName dnsName) throws DnsNotFoundException, DnsException {
    try {
      return this
        .getLookupSession()
        .lookupAsync(getXbillName(dnsName), Type.MX)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(MXRecord.class::cast)
        .map(mx -> {

          XBillDnsClient client = this;
          return new DnsMxRecord() {
            @Override
            public int getPriority() {
              return mx.getPriority();
            }

            @Override
            public DnsName getTarget() {
              try {
                return client.createDnsName(mx.getName().toString());
              } catch (DnsIllegalArgumentException e) {
                throw new RuntimeException(e);
              }
            }
          };

        })
        .collect(Collectors.toList());
    } catch (Exception e) {
      throw this.handleLookupException(dnsName, e);
    }
  }

  @Override
  public DnsIp lookupA(DnsName dnsName) {
    return null;
  }

  @Override
  public DnsIp lookupAAAA(DnsName dnsName) {
    return null;
  }

  @Override
  public String lookupTxt(DnsName dnsName) throws DnsException, DnsNotFoundException {
    List<String> txtRecords = this.resolveTxt(dnsName);
    if (txtRecords.size() == 0) {
      throw new DnsNotFoundException();
    }
    return txtRecords.get(0);
  }

  @Override
  public Set<DnsIp> resolveAAAA(DnsName dnsName) throws DnsNotFoundException, DnsException {
    try {
      return this.getLookupSession().lookupAsync(getXbillName(dnsName), Type.AAAA)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(AAAARecord.class::cast)
        .map(r -> this.createIpFromAddress(r.getAddress()))
        .collect(Collectors.toSet());
    } catch (Exception e) {
      throw this.handleLookupException(dnsName, e);
    }
  }


  public static class DnsClientBuilder {

    private Resolver resolver = DnsResolver.getLocal();

    public DnsClientBuilder setResolverToCloudflare() {
      this.resolver = DnsResolver.getCloudflare();
      return this;
    }

    public XBillDnsClient build() {
      return new XBillDnsClient(this);
    }
  }
}
