package net.bytle.dns;

import org.xbill.DNS.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DnsDomain {
  private final String domainNameString;
  private final Resolver resolver;
  private final Name absoluteDomainNameName;

  public DnsDomain(String domainNameString) {
    this.resolver = DnsResolver.getLocal();
    this.domainNameString = domainNameString;
    this.absoluteDomainNameName = DnsName.createFrom(domainNameString).toAbsolute();
  }

  public static DnsDomain createFrom(String domainName) {
    return new DnsDomain(domainName);
  }

  public  TXTRecord getDkimRecord(String dkimSelector) throws IOException {
    return getTextRecordThatStartsWith(this.getDkimName(dkimSelector),getDkimPrefix());
  }

  private String getDkimPrefix() {
    return "v=DKIM1";
  }


  public List<MXRecord> getMxRecords() throws TextParseException {

    return Arrays.stream(new Lookup(this.domainNameString, Type.MX).run()).map(MXRecord.class::cast).collect(Collectors.toList());

  }

  public List<MXRecord> getMxRecordsAsync() throws ExecutionException, InterruptedException {

    Record queryRecord = Record.newRecord(absoluteDomainNameName, Type.MX, DClass.IN);

    Message queryMessage = Message.newQuery(queryRecord);

    Message responseMessage = resolver.sendAsync(queryMessage)
      .whenComplete(
        (answer, ex) -> {
          if (ex != null) {
            ex.printStackTrace();
          }
        })
      .toCompletableFuture()
      .get();
    return responseMessage.getSection(1).stream().map(MXRecord.class::cast).collect(Collectors.toList());

  }

  /**
   * Collect all SPF record for the domain
   * @param startsWith - the begins of the text
   * @return the record
   * @throws NoSuchElementException when not found
   */
  @SuppressWarnings("unused")
  public TXTRecord getTextRecordThatStartsWith(String startsWith) throws NoSuchElementException, IOException {

    return getTextRecordThatStartsWith(this.absoluteDomainNameName, startsWith);

  }

  /**
   * @param name  - the name to lookup
   * @param startsWith - the prefix
   */
  private TXTRecord getTextRecordThatStartsWith(Name name, String startsWith) throws NoSuchElementException, IOException {

    Record queryRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    Message queryMessage = Message.newQuery(queryRecord);
    return this.resolver
      .send(queryMessage)
      .getSection(1)
      .stream()
      .map(TXTRecord.class::cast)
      .filter(record -> DnsUtil.getStringFromTxtRecord(record).startsWith(startsWith))
      .findFirst()
      .orElseThrow(() -> new NoSuchElementException("The (" + startsWith + ") text record for the name (" + name + ") was not found"));

  }


  public TXTRecord getSpfRecord() throws IOException {

    return getTextRecordThatStartsWith(this.absoluteDomainNameName, getSpfPrefix());

  }

  public String getSpfARecordName(){
    return "spf." + this.domainNameString;
  }


  public static String getSpfPrefix() {
    return "v=spf1";
  }

  private String getDkimSelector(String dkimSelector) {
    return  dkimSelector + "._domainkey." + this.domainNameString;  }

  public Name getDkimName(String dkimSelector) {
    return DnsName.createFrom(getDkimSelector(dkimSelector)).toAbsolute();
  }

  public Name getDmarcName() {
    String dmarcSelector = "_dmarc";
    return DnsName.createFrom(dmarcSelector + "." + this.domainNameString).toAbsolute();
  }

  public TXTRecord getDmarcRecord() throws IOException {
    return getTextRecordThatStartsWith(getDmarcName(), getDmarcPrefix());
  }

  public static String getDmarcPrefix() {
    return "v=DMARC1";
  }

  public String getName() {
    return this.domainNameString;
  }

  @Override
  public String toString() {
    return domainNameString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DnsDomain dnsDomain = (DnsDomain) o;
    return Objects.equals(absoluteDomainNameName, dnsDomain.absoluteDomainNameName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(absoluteDomainNameName);
  }
}
