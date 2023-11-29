package net.bytle.dns;

import net.bytle.email.BMailInternetAddress;

import java.util.*;
import java.util.stream.Collectors;

public class DnsName {

  private static final String DNS_SEPARATOR = ".";
  public static final String ROOT_DOT = DNS_SEPARATOR;

  private final String absoluteDnsName;
  private final DnsClient client;
  /**
   * Selector / Value
   */
  private final Map<String, String> expectedDkims = new HashMap<>();
  private final List<BMailInternetAddress> expectedDmarcEmails = new ArrayList<>();

  protected DnsName(DnsClient client, String absoluteName) throws DnsIllegalArgumentException {
    if (!absoluteName.endsWith(ROOT_DOT)) {
      this.absoluteDnsName = absoluteName + ROOT_DOT;
    } else {
      this.absoluteDnsName = absoluteName;
    }
    this.client = client;
  }


  /**
   * For a name, you may get multiple record with the same name
   * but two differents address
   * For instance:
   * * when the address is proxied by cloudflare)
   * * for a list of host (mailers)
   * We return the first one
   */
  public DnsIp lookupA() throws DnsNotFoundException, DnsException {

    Set<DnsIp> aRecords = this.resolveA();
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

  public DnsIp lookupAAAA() throws DnsNotFoundException, DnsException {

    Set<DnsIp> aaaaRecords = resolveAAAA();
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
    return this.absoluteDnsName.contains(dnsName.absoluteDnsName);
  }


  public String getDkimRecord(String dkimSelector) throws DnsException, DnsNotFoundException {

    String dkimSelectorName = dkimSelector + "._domainkey." + this.absoluteDnsName;
    try {
      return this.client.createDnsName(dkimSelectorName).getTextRecordThatStartsWith("v=DKIM1");
    } catch (DnsIllegalArgumentException e) {
      throw new RuntimeException(e);
    }

  }


  public List<DnsMxRecord> getMxRecords() throws DnsException, DnsNotFoundException {

    return this.client.resolveMx(this);

  }


  /**
   * @param startsWith - the prefix
   */
  String getTextRecordThatStartsWith(String startsWith) throws DnsNotFoundException, DnsException {


    return this.getTxtRecords()
      .stream()
      .filter(record -> record.startsWith(startsWith))
      .findFirst()
      .orElseThrow(() -> new DnsNotFoundException("The (" + startsWith + ") text record for the name (" + absoluteDnsName + ") was not found"));


  }

  private List<String> getTxtRecords() throws DnsNotFoundException, DnsException {
    return this.client.resolveTxt(this);
  }


  public String getSpfRecord() throws DnsException, DnsNotFoundException {

    return getTextRecordThatStartsWith("v=spf1");

  }


  public String getDmarcRecord() throws DnsException, DnsNotFoundException {
    try {
      return getSubdomain("_dmarc").getTextRecordThatStartsWith("v=DMARC1");
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
      dnsIp = lookupA();
    } catch (DnsNotFoundException e) {
      dnsIp = lookupAAAA();
    }
    return dnsIp;
  }

  public DnsIp getFirstDnsIpv4Address() throws DnsException, DnsNotFoundException {
    return lookupA();
  }


  @Override
  public String toString() {
    return this.absoluteDnsName;
  }


  public DnsIp lookupIpv6() throws DnsException, DnsNotFoundException {
    return lookupAAAA();
  }

  public DnsName getSubdomain(String label) throws DnsIllegalArgumentException {
    return new DnsName(this.client, label + DNS_SEPARATOR + this.absoluteDnsName);
  }

  /**
   * In Spf record, the name does not have any root separator
   */
  public String toStringWithoutRoot() {
    return this.absoluteDnsName.substring(0, this.absoluteDnsName.length() - 1);
  }

  public Set<DnsIp> resolveA() throws DnsException, DnsNotFoundException {
    return this.client.resolveA(this);
  }

  public DnsName getApexName() {
    String[] labels = this.absoluteDnsName.split("\\.");
    int labelSize = labels.length;
    if (labelSize <= 3) {
      return this;
    }
    try {
      String rootLabel = labels[labelSize - 1];
      String tldLabel = labels[labelSize - 2];
      String apexLabel = labels[labelSize - 3];
      return new DnsName(this.client, apexLabel + "." + tldLabel + "." + rootLabel);
    } catch (DnsIllegalArgumentException e) {
      throw new DnsInternalException("It should not throw");
    }
  }

  public Set<DnsName> getCNameRecords() throws DnsNotFoundException, DnsException {
    return this.client.lookupCName(this);
  }

  public Set<DnsIp> resolveAAAA() throws DnsNotFoundException, DnsException {
    return this.client.resolveAAAA(this);
  }

  public void printRecord() {

    System.out.println("DNS Records for " + this);
    String tabLevel1 = "  - ";
    String tabLevel2 = "     - ";
    try {
      System.out.println(tabLevel1 + "Ipv4 (A records)");
      Set<DnsIp> aRecords = this.resolveA();
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
      Set<DnsIp> dnsIpv6s = this.resolveAAAA();
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
      Set<DnsName> cnameRecords = this.getCNameRecords();
      if (cnameRecords.size() == 0) {
        System.out.println(tabLevel2 + "none");
      }
      for (DnsName cnameRecord : cnameRecords) {
        System.out.println(tabLevel2 + cnameRecord.toString());
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No domain record");
    }
  }

  public void addExpectedDkim(String selector, String value) {
    this.expectedDkims.put(selector, value);
  }

  public Set<String> getExpectedDkimSelector() {
    return this.expectedDkims.keySet();
  }

  public String getExpectedDkimValue(String selector) {
    return this.expectedDkims.get(selector);
  }

  public String getExpectedDmarcRecord() {
    String dmarc = "v=DMARC1";
    /**
     * Policy (none, quarantine, reject)
     * If your domain uses BIMI, the DMARC p option must be set to quarantine or reject.
     * BIMI doesn't support DMARC policies with the p option set to none.
     */
    String rejectPolicy = "none";
    dmarc += "; p=" + rejectPolicy;
    /**
     * #########################
     * Percentage of unauthorized messages
     * BIMI doesn't support DMARC policies with the pct value set to less than 100.
     * dmarc += "; pct=100";
     * #########################
     * Subdomain Policy (sp)
     * Optional - Inherited from p
     * dmarc += "; sp=" + rejectPolicy;
     */
    if (this.expectedDmarcEmails.size() == 0) {
      return dmarc;
    }
    String mailToSchema = "mailto:";
    /**
     * We put a space as shown in google here:
     * https://support.google.com/a/answer/2466563#dmarc-record-tags
     */

    String ruaDelimiter = ", " + mailToSchema;
    return dmarc + "; rua=" + mailToSchema + this.expectedDmarcEmails.stream().map(BMailInternetAddress::getAddress).collect(Collectors.joining(ruaDelimiter));
  }

  public DnsName addExpectedDmarcEmail(BMailInternetAddress mail) {
    this.expectedDmarcEmails.add(mail);
    return this;
  }

  public List<BMailInternetAddress> getDmarcEmails() {
    return this.expectedDmarcEmails;
  }

  public String lookupTxt() throws DnsException, DnsNotFoundException {
    return this.client.lookupTxt(this);
  }

  public String getAbsoluteName() {
    return this.absoluteDnsName;
  }


}
