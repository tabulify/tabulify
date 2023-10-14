package net.bytle.dns;

import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DnsName {

  private static final String DNS_SEPARATOR = ".";
  public static final String ROOT_DOT = DNS_SEPARATOR;

  private final String absoluteDnsName;
  private final DnsSession session;
  private final Name dnsName;

  protected DnsName(DnsSession session, String absoluteName) throws DnsIllegalArgumentException {
    if (!absoluteName.endsWith(ROOT_DOT)) {
      this.absoluteDnsName = absoluteName + ROOT_DOT;
    } else {
      this.absoluteDnsName = absoluteName;
    }
    try {
      this.dnsName = Name.fromString(this.absoluteDnsName);
    } catch (TextParseException e) {
      throw new DnsIllegalArgumentException(e);
    }
    this.session = session;
  }


  /**
   * For a name, you may get multiple record with the same name
   * but two differents address
   * For instance:
   * * when the address is proxied by cloudflare)
   * * for a list of host (mailers)
   * We return the first one
   */
  public DnsIp getFirstARecord() throws DnsNotFoundException, DnsException {

    Set<DnsIp> aRecords = this.getARecords();
    int size = aRecords.size();
    switch (size) {
      case 0:
        throw new DnsNotFoundException("There is no A record for the name (" + this + ")");
      default:
        /**
         * We may get 2 records with the same name but with 2 differents addresses
         * (for instance when the address is proxied by cloudflare)
         */
        return aRecords.iterator().next();
    }

  }

  public DnsIp getFirstAAAARecord() throws DnsNotFoundException, DnsException {

    Set<DnsIp> aaaaRecords = getAAAARecords();
    switch (aaaaRecords.size()) {
      case 0:
        throw new DnsNotFoundException("There is no AAAA record for the name (" + this + ")");
      default:
        /**
         * Due to load balancer, we may get more than one
         */
        return aaaaRecords.iterator().next();
    }

  }


  @SuppressWarnings("unused")
  boolean isSubdomain(DnsName dnsName) {
    return this.dnsName.subdomain(dnsName.dnsName);
  }


  /**
   * @return subdomains (A and AAAA record)
   */
  @SuppressWarnings("unused")
  public List<Record> getSubdomains() {


    CompletableFuture<LookupResult> AFutureRecords = session
      .getLookupSession()
      .lookupAsync(this.dnsName, Type.A)
      .toCompletableFuture();
    CompletableFuture<LookupResult> AAAAFutureRecords = session
      .getLookupSession()
      .lookupAsync(this.dnsName, Type.AAAA)
      .toCompletableFuture();


    return Stream.of(AFutureRecords, AAAAFutureRecords)
      .map(CompletableFuture::join)
      .flatMap(res -> res.getRecords().stream())
      .collect(Collectors.toList());


  }


  /**
   * Just an alias
   */
  @SuppressWarnings("unused")
  public DnsIp forwardLookup() throws DnsException, DnsNotFoundException {

    return getFirstARecord();

  }

  public String getDkimRecord(String dkimSelector) throws DnsException, DnsNotFoundException {

    String dkimSelectorName = dkimSelector + "._domainkey." + this.absoluteDnsName;
    DnsName dkimDnsName;
    try {
      dkimDnsName = this.session.createDnsName(dkimSelectorName);
    } catch (DnsIllegalArgumentException e) {
      throw new DnsInternalException(e);
    }
    return DnsUtil.getStringFromTxtRecord(dkimDnsName.getTextRecordThatStartsWith("v=DKIM1"));

  }


  public List<MXRecord> getMxRecords() throws DnsException, DnsNotFoundException {


    try {
      return this.session
        .getLookupSession()
        .lookupAsync(dnsName, Type.MX)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(MXRecord.class::cast)
        .collect(Collectors.toList()
        );
    } catch (Exception e) {
      throw this.session.handleLookupException(this, e);
    }

  }


  /**
   * @param startsWith - the prefix
   */
  TXTRecord getTextRecordThatStartsWith(String startsWith) throws DnsNotFoundException, DnsException {


    try {
      return this.session
        .getLookupSession()
        .lookupAsync(this.dnsName, Type.TXT)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(TXTRecord.class::cast)
        .filter(record -> DnsUtil.getStringFromTxtRecord(record).startsWith(startsWith))
        .findFirst()
        .orElseThrow(() -> new DnsNotFoundException("The (" + startsWith + ") text record for the name (" + absoluteDnsName + ") was not found"));
    } catch (Exception e) {
      throw this.session.handleLookupException(this, e);
    }

  }


  public String getSpfRecord() throws DnsException, DnsNotFoundException {

    return DnsUtil.getStringFromTxtRecord(getTextRecordThatStartsWith("v=spf1"));

  }


  public String getDmarcRecord() throws DnsException, DnsNotFoundException {
    try {
      return DnsUtil.getStringFromTxtRecord(getSubdomain("_dmarc").getTextRecordThatStartsWith("v=DMARC1"));
    } catch (DnsIllegalArgumentException e) {
      throw new DnsInternalException("_dmarc is a valid name", e);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DnsName dnsDomain = (DnsName) o;
    return Objects.equals(absoluteDnsName, dnsDomain.absoluteDnsName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(absoluteDnsName);
  }

  /**
   * A name may have several ip due to:
   * * proxy (Cloudflare , ...)
   * * load balancing
   * ...
   */
  public DnsIp getFirstDnsIpAddress() throws DnsException, DnsNotFoundException {
    DnsIp dnsIp;
    try {
      dnsIp = getFirstARecord();
    } catch (DnsNotFoundException e) {
      dnsIp = getFirstAAAARecord();
    }
    return dnsIp;
  }

  public DnsIp getFirstDnsIpv4Address() throws DnsException, DnsNotFoundException {
    return getFirstARecord();
  }


  @Override
  public String toString() {
    return dnsName.toString();
  }

  public String getName() {
    return this.absoluteDnsName;
  }

  public DnsIp getFirstDnsIpv6Address() throws DnsException, DnsNotFoundException {
    return getFirstAAAARecord();
  }

  public DnsName getSubdomain(String label) throws DnsIllegalArgumentException {
    return new DnsName(this.session, label + DNS_SEPARATOR + this.absoluteDnsName);
  }

  /**
   * In Spf record, the name does not have any root separator
   */
  public String getNameWithoutRoot() {
    return this.absoluteDnsName.substring(0, this.absoluteDnsName.length() - 1);
  }

  public Set<DnsIp>
  getARecords() throws DnsException, DnsNotFoundException {
    try {
      return session.getLookupSession().lookupAsync(this.dnsName, Type.A)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream().map(ARecord.class::cast)
        .map(ARecord::getAddress)
        .map(this.session::createIpFromAddress)
        .collect(Collectors.toSet());
    } catch (Exception e) {
      throw this.session.handleLookupException(this, e);
    }
  }

  public DnsName getApexName() {
    int labels = this.dnsName.labels();
    if (labels <= 3) {
      return this;
    }
    try {
      String rootLabel = this.dnsName.getLabelString(labels - 1);
      String tldLabel = this.dnsName.getLabelString(labels - 2);
      String apexLabel = this.dnsName.getLabelString(labels - 3);
      return new DnsName(this.session, apexLabel + "." + tldLabel + "." + rootLabel);
    } catch (DnsIllegalArgumentException e) {
      throw new DnsInternalException("It should not throw");
    }
  }

  public Set<CNAMERecord> getCNameRecords() throws DnsNotFoundException, DnsException {
    try {
      return session.getLookupSession()
        .lookupAsync(this.dnsName, Type.CNAME)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream().map(CNAMERecord.class::cast)
        .collect(Collectors.toSet());
    } catch (Exception e) {
      throw this.session.handleLookupException(this, e);
    }
  }

  public Set<DnsIp> getAAAARecords() throws DnsNotFoundException, DnsException {
    try {
      return session.getLookupSession().lookupAsync(this.dnsName, Type.AAAA)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(AAAARecord.class::cast)
        .map(r -> this.session.createIpFromAddress(r.getAddress()))
        .collect(Collectors.toSet());
    } catch (Exception e) {
      throw this.session.handleLookupException(this, e);
    }
  }

  public void printRecord() {

    System.out.println("DNS Records for " + this);
    String tabLevel1 = "  - ";
    String tabLevel2 = "     - ";
    try {
      System.out.println(tabLevel1 + "Ipv4 (A records)");
      Set<DnsIp> aRecords = this.getARecords();
      for (DnsIp dnsIp : aRecords) {
        System.out.println(tabLevel2 + dnsIp);
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No A record");
    }


    try {
      System.out.println(tabLevel1 + "Ipv6 (AAAA records)");
      Set<DnsIp> dnsIpv6s = this.getAAAARecords();
      if (dnsIpv6s.size() == 0) {
        System.out.println(tabLevel2 + "none");
      }
      for (DnsIp dnsIp : dnsIpv6s) {
        System.out.println(tabLevel2 + dnsIp);
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No AAAA record");
    }

    try {
      System.out.println(tabLevel1 + "Cname for " + this);
      Set<CNAMERecord> cnameRecords = this.getCNameRecords();
      if (cnameRecords.size() == 0) {
        System.out.println(tabLevel2 + "none");
      }
      for (CNAMERecord cnameRecord : cnameRecords) {
        System.out.println(tabLevel2 + cnameRecord.getTarget().toString(true));
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No domain record");
    }
  }
}
