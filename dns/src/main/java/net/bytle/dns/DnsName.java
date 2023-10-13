package net.bytle.dns;

import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
   * (for instance when the address is proxied by cloudflare)
   * We return the first one
   */
  public ARecord getFirstARecord() throws DnsNotFoundException, DnsException {

    List<ARecord> aRecords;
    try {
      aRecords = session.getLookupSession().lookupAsync(this.dnsName, Type.A)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream().map(ARecord.class::cast)
        .collect(Collectors.toList());
    } catch (InterruptedException | ExecutionException e) {
      throw new DnsException(e);
    }
    int size = aRecords.size();
    switch (size) {
      case 0:
        throw new DnsNotFoundException("There is no A record for the name (" + this + ")");
      default:
        /**
         * We may get 2 records with the same name but with 2 differents addresses
         * (for instance when the address is proxied by cloudflare)
         */
        return aRecords.get(0);
    }

  }

  public AAAARecord getFirstAAAARecord() throws DnsNotFoundException, DnsException {

    List<AAAARecord> aaaaRecords;
    try {
      aaaaRecords = session.getLookupSession().lookupAsync(this.dnsName, Type.AAAA)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream().map(AAAARecord.class::cast)
        .collect(Collectors.toList());
    } catch (InterruptedException | ExecutionException e) {
      throw new DnsException(e);
    }
    switch (aaaaRecords.size()) {
      case 0:
        throw new DnsNotFoundException("There is no AAAA record for the name (" + this + ")");
      default:
        /**
         * Due to load balancer, we may get more than one
         */
        return aaaaRecords.get(0);
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
   *
   * Just an alias
   */
  @SuppressWarnings("unused")
  public ARecord forwardLookup() throws DnsException, DnsNotFoundException {

    return getFirstARecord();

  }

  public TXTRecord getDkimRecord(String dkimSelector) throws DnsException, DnsNotFoundException {

    String dkimSelectorName = dkimSelector + "._domainkey." + this.absoluteDnsName;
    DnsName dkimDnsName;
      try {
        dkimDnsName = this.session.createDnsName(dkimSelectorName);
      } catch (DnsIllegalArgumentException e) {
        throw new DnsInternalException(e);
      }
    return dkimDnsName.getTextRecordThatStartsWith("v=DKIM1");

  }




  public List<MXRecord> getMxRecords() throws DnsException {


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
    } catch (InterruptedException | ExecutionException e) {
      throw new DnsException(e);
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

    } catch (ExecutionException| InterruptedException e) {
      throw new DnsException(e);
    }

  }


  public String getSpfRecord() throws DnsException, DnsNotFoundException {

    return DnsUtil.getStringFromTxtRecord(getTextRecordThatStartsWith("v=spf1"));

  }





  public DnsName getDmarcName()  {
    String dmarcSelector = "_dmarc";
    try {
      return this.session.createDnsName(dmarcSelector + "." + this.absoluteDnsName);
    } catch (DnsIllegalArgumentException e) {
      throw new DnsInternalException(e);
    }
  }

  public TXTRecord getDmarcRecord() throws DnsException, DnsNotFoundException {
    return getDmarcName().getTextRecordThatStartsWith("v=DMARC1");
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
    InetAddress inetAddress;
    try {
       inetAddress = getFirstARecord().getAddress();
    } catch (DnsNotFoundException e) {
      inetAddress = getFirstAAAARecord().getAddress();
    }
    return  this.session.createIpFromAddress(inetAddress);
  }

  public DnsIp getFirstDnsIpv4Address() throws DnsException, DnsNotFoundException {
    InetAddress inetAddress = getFirstARecord().getAddress();
    return  this.session.createIpFromAddress(inetAddress);
  }


  @Override
  public String toString() {
    return dnsName.toString();
  }

  public String getName() {
    return this.absoluteDnsName;
  }

  public DnsIp getFirstDnsIpv6Address() throws DnsException, DnsNotFoundException {
    InetAddress inetAddress = getFirstAAAARecord().getAddress();
    return  this.session.createIpFromAddress(inetAddress);
  }

  public DnsName getSubdomain(String label) throws DnsIllegalArgumentException {
    return new DnsName(this.session, label+ DNS_SEPARATOR + this.absoluteDnsName);
  }

  /**
   *
   * In Spf record, the name does not have any root separator
   */
  public String getNameWithoutRoot() {
    return this.absoluteDnsName.substring(0,this.absoluteDnsName.length()-1);
  }
}
