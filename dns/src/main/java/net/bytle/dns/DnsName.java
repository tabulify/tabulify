package net.bytle.dns;

import net.bytle.exception.NotFoundException;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DnsName {


  public static final String ROOT_DOT = ".";
  private final String name;
  private final DnsSession session;
  private final Name dnsName;

  protected DnsName(DnsSession session, String absoluteName) throws TextParseException {
    if (!absoluteName.endsWith(ROOT_DOT)) {
      this.name = absoluteName + ROOT_DOT;
    } else {
      this.name = absoluteName;
    }
    this.dnsName = Name.fromString(this.name);
    this.session = session;
  }


  /**
   * For a name, you may get multiple record with the same name
   * but two differents address
   * (for instance when the address is proxied by cloudflare)
   * We return the first one
   */
  public ARecord getFirstARecord() throws NotFoundException, DnsException {

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
        throw new NotFoundException("There is no A record for the name (" + this + ")");
      default:
        /**
         * We may get 2 records with the same name but with 2 differents addresses
         * (for instance when the address is proxied by cloudflare)
         */
        return aRecords.get(0);
    }

  }

  public AAAARecord getAAAARecord() throws NotFoundException, DnsException {

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
        throw new NotFoundException("There is more than one AAAA record for the name (" + this + ")");
      case 1:
        return aaaaRecords.get(0);
      default:
        throw new DnsException("There is more than one AAAA record for the name (" + this + ")");
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
  public ARecord forwardLookup() throws DnsException, NotFoundException {

    return getFirstARecord();

  }

  public TXTRecord getDkimRecord(String dkimSelector) throws ExecutionException, InterruptedException, TextParseException {
    return this.getDkimName(dkimSelector).getTextRecordThatStartsWith(getDkimPrefix());
  }

  private String getDkimPrefix() {
    return "v=DKIM1";
  }


  public List<MXRecord> getMxRecords() throws ExecutionException, InterruptedException {


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

  }


  /**
   * @param startsWith - the prefix
   */
  TXTRecord getTextRecordThatStartsWith(String startsWith) throws NoSuchElementException, ExecutionException, InterruptedException {


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
      .orElseThrow(() -> new NoSuchElementException("The (" + startsWith + ") text record for the name (" + name + ") was not found"));

  }


  public TXTRecord getSpfRecord() throws ExecutionException, InterruptedException {

    return getTextRecordThatStartsWith(getSpfPrefix());

  }

  public String getSpfARecordName() {
    return "spf." + this.name;
  }


  public static String getSpfPrefix() {
    return "v=spf1";
  }

  private String getDkimSelectorName(String dkimSelector) {
    return dkimSelector + "._domainkey." + this.name;
  }

  public DnsName getDkimName(String dkimSelector) throws TextParseException {
    return this.session.createDnsName(getDkimSelectorName(dkimSelector));
  }

  public DnsName getDmarcName() throws TextParseException {
    String dmarcSelector = "_dmarc";
    return this.session.createDnsName(dmarcSelector + "." + this.name);
  }

  public TXTRecord getDmarcRecord() throws IOException, ExecutionException, InterruptedException {
    return getDmarcName().getTextRecordThatStartsWith(getDmarcPrefix());
  }

  public static String getDmarcPrefix() {
    return "v=DMARC1";
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DnsName dnsDomain = (DnsName) o;
    return Objects.equals(name, dnsDomain.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  /**
   * A name may have several ip due to:
   * * proxy (Cloudflare , ...)
   * * load balancing
   * ...
   */
  public DnsIp getFirstDnsIpAddress() throws DnsException, NotFoundException {
    InetAddress inetAddress;
    try {
       inetAddress = getFirstARecord().getAddress();
    } catch (NotFoundException e) {
      inetAddress = getAAAARecord().getAddress();
    }
    return  this.session.createIpFromAddress(inetAddress);
  }


  @Override
  public String toString() {
    return dnsName.toString();
  }

}
