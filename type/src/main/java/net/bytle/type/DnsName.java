package net.bytle.type;


import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DNS Name is a basic type
 * <p>
 * It's used everywhere from URI to Email.
 * <p>
 * Used in Kubernetes as
 * <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/names#dns-subdomain-names">name structure</a>
 */
public class DnsName {

  private static final String DNS_SEPARATOR = ".";
  public static final String ROOT_DOT = DNS_SEPARATOR;

  private final DnsNameXbill xBillDnsName;
  /**
   * Selector / Value
   */
  private final Map<String, String> expectedDkims = new HashMap<>();
  private final List<EmailAddress> expectedDmarcEmails = new ArrayList<>();

  protected DnsName(String absoluteName) throws DnsCastException {
    String nameWithRoot;
    if (!absoluteName.endsWith(ROOT_DOT)) {
      nameWithRoot = absoluteName + ROOT_DOT;
    } else {
      nameWithRoot = absoluteName;
    }
    this.xBillDnsName = DnsNameXbill.fromString(nameWithRoot);
  }

  /**
   *
   * @param absoluteName - an absolute name with or without the root dot (We add it)
   * @return a DNS Name
   * @throws DnsCastException if the name is not valid
   */
  public static DnsName create(String absoluteName) throws DnsCastException {
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


  /**
   * @return the domain without the root point
   */
  @Override
  public String toString() {
    /**
     * When used as a string, there is no root dot
     * always
     */
    return toStringWithoutRoot();
  }


  public DnsName getSubdomain(String label) throws CastException {
    return new DnsName(label + DNS_SEPARATOR + this.xBillDnsName);
  }

  /**
   * In Spf record, the name does not have any root separator
   */
  public String toStringWithoutRoot() {
    return this.xBillDnsName.toString(true);
  }

  @SuppressWarnings("unused")
  public String toStringWithRoot() {
    return this.xBillDnsName.toString(false);
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
      return new DnsName(apexLabel + "." + tldLabel + "." + rootLabel);
    } catch (CastException e) {
      throw new InternalException("It should not throw");
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
    return dmarc + "; rua=" + mailToSchema + this.expectedDmarcEmails.stream().map(EmailAddress::toString).collect(Collectors.joining(ruaDelimiter));
  }

  @SuppressWarnings("UnusedReturnValue")
  public DnsName addExpectedDmarcEmail(EmailAddress mail) {
    this.expectedDmarcEmails.add(mail);
    return this;
  }

  public List<EmailAddress> getDmarcEmails() {
    return this.expectedDmarcEmails;
  }


  public List<String> getLabels() {
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < this.xBillDnsName.labels(); i++) {
      labels.add(this.xBillDnsName.getLabelString(i));
    }
    return labels;
  }

  public boolean isApexDomain() {
    return getApexName().equals(this);
  }


}
