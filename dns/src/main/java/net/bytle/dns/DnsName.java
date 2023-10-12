package net.bytle.dns;

import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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


  public InetAddress getIpAddress() throws UnknownHostException {
    return Address.getByName(name);
  }


  public ARecord getARecord() throws ExecutionException, InterruptedException {


    return session.getLookupSession().lookupAsync(this.dnsName,Type.A)
      .toCompletableFuture()
      .get()
      .getRecords()
      .stream().map(ARecord.class::cast)
      .findFirst()
      .orElseThrow();

  }


  @SuppressWarnings("unused")
  public ARecord forwardLookup() throws ExecutionException, InterruptedException {

    return getARecord();

  }

  public TXTRecord getDkimRecord(String dkimSelector) throws ExecutionException, InterruptedException, TextParseException {
    return this.getDkimName(dkimSelector).getTextRecordThatStartsWith( getDkimPrefix());
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
}
