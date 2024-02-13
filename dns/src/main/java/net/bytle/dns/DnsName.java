package net.bytle.dns;


import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.*;
import java.util.stream.Collectors;

public class DnsName {

  private static final String DNS_SEPARATOR = ".";
  public static final String ROOT_DOT = DNS_SEPARATOR;

  private final Name xBillDnsName;
  /**
   * Selector / Value
   */
  private final Map<String, String> expectedDkims = new HashMap<>();
  private final List<DnsEmailAddress> expectedDmarcEmails = new ArrayList<>();

  protected DnsName(String absoluteName) throws DnsIllegalArgumentException {
    String nameWithRoot;
    if (!absoluteName.endsWith(ROOT_DOT)) {
      nameWithRoot = absoluteName + ROOT_DOT;
    } else {
      nameWithRoot = absoluteName;
    }
    try {
      this.xBillDnsName = Name.fromString(nameWithRoot);
    } catch (TextParseException e) {
      throw new DnsIllegalArgumentException(e);
    }

  }

  public static DnsName create(String absoluteName) throws DnsIllegalArgumentException {
    return new DnsName(absoluteName);
  }



  @SuppressWarnings("unused")
  boolean isSubdomain(DnsName dnsName) {
    return this.xBillDnsName.subdomain(dnsName.xBillDnsName);
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DnsName dnsDomain = (DnsName) o;
    return Objects.equals(xBillDnsName, dnsDomain.xBillDnsName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(xBillDnsName);
  }



  @Override
  public String toString() {
    return this.xBillDnsName.toString();
  }


  public DnsName getSubdomain(String label) throws DnsIllegalArgumentException {
    return new DnsName( label + DNS_SEPARATOR + this.xBillDnsName);
  }

  /**
   * In Spf record, the name does not have any root separator
   */
  public String toStringWithoutRoot() {
    return this.xBillDnsName.toString(true);
  }

  public DnsName getApexName() {
    int labels = this.xBillDnsName.labels();
    if (labels <= 3) {
      return this;
    }
    try {
      String rootLabel = this.xBillDnsName.getLabelString(labels - 1);
      String tldLabel = this.xBillDnsName.getLabelString(labels - 2);
      String apexLabel = this.xBillDnsName.getLabelString(labels - 3);
      return new DnsName( apexLabel + "." + tldLabel + "." + rootLabel);
    } catch (DnsIllegalArgumentException e) {
      throw new DnsInternalException("It should not throw");
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
    if (this.expectedDmarcEmails.isEmpty()) {
      return dmarc;
    }
    String mailToSchema = "mailto:";
    /**
     * We put a space as shown in google here:
     * https://support.google.com/a/answer/2466563#dmarc-record-tags
     */
    String ruaDelimiter = ", " + mailToSchema;
    return dmarc + "; rua=" + mailToSchema + this.expectedDmarcEmails.stream().map(DnsEmailAddress::toString).collect(Collectors.joining(ruaDelimiter));
  }

  public DnsName addExpectedDmarcEmail(DnsEmailAddress mail) {
    this.expectedDmarcEmails.add(mail);
    return this;
  }

  public List<DnsEmailAddress> getDmarcEmails() {
    return this.expectedDmarcEmails;
  }


  public List<String> getLabels() {
    List<String> labels = new ArrayList<>();
    for(int i=0;i<this.xBillDnsName.labels(); i++){
      labels.add(this.xBillDnsName.getLabelString(i));
    }
    return labels;
  }
}
