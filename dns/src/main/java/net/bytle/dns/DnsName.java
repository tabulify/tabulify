package net.bytle.dns;

import net.bytle.email.BMailInternetAddress;

import java.util.*;
import java.util.stream.Collectors;

public class DnsName {

  private static final String DNS_SEPARATOR = ".";
  public static final String ROOT_DOT = DNS_SEPARATOR;

  private final String absoluteDnsName;
  /**
   * Selector / Value
   */
  private final Map<String, String> expectedDkims = new HashMap<>();
  private final List<BMailInternetAddress> expectedDmarcEmails = new ArrayList<>();

  protected DnsName(String absoluteName) throws DnsIllegalArgumentException {
    if (!absoluteName.endsWith(ROOT_DOT)) {
      this.absoluteDnsName = absoluteName + ROOT_DOT;
    } else {
      this.absoluteDnsName = absoluteName;
    }
  }

  public static DnsName create(String absoluteName) throws DnsIllegalArgumentException {
    return new DnsName(absoluteName);
  }





  @SuppressWarnings("unused")
  boolean isSubdomain(DnsName dnsName) {
    return this.absoluteDnsName.contains(dnsName.absoluteDnsName);
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



  @Override
  public String toString() {
    return this.absoluteDnsName;
  }


  public DnsName getSubdomain(String label) throws DnsIllegalArgumentException {
    return new DnsName( label + DNS_SEPARATOR + this.absoluteDnsName);
  }

  /**
   * In Spf record, the name does not have any root separator
   */
  public String toStringWithoutRoot() {
    return this.absoluteDnsName.substring(0, this.absoluteDnsName.length() - 1);
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
      return new DnsName(apexLabel + "." + tldLabel + "." + rootLabel);
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


  public String getAbsoluteName() {
    return this.absoluteDnsName;
  }


}
