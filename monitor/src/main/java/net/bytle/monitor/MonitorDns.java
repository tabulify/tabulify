package net.bytle.monitor;

import net.bytle.dns.*;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * To finish it, see
 * <a href="https://support.google.com/mail/answer/81126">...</a>
 */
public class MonitorDns {


  private final DnsSession dnsSession;

  public MonitorDns() {

    this.dnsSession = DnsSession.builder()
      // we are at cloudflare, no need to wait
      .setResolverToCloudflare()
      .build();
  }


  public static MonitorDns create() {
    return new MonitorDns();
  }


  /**
   * Public PTR is mandatory to receive email
   * Mail servers will not accept mail from IP addresses with no rDNS/PTR record or a generic PTR record.
   * <p>
   * See:
   * The IP address sending this
   * message does 550-5.7.25 not have a PTR record setup, or the corresponding
   * forward DNS entry 550-5.7.25 does not point to the sending IP. As a policy,
   * Gmail does not accept 550-5.7.25 messages from IPs with missing PTR
   * records. Please visit 550-5.7.25
   * <a href="https://support.google.com/mail/answer/81126#ip-practices">...</a> for more 550
   */

  public MonitorReport checkHostPtr(MonitorNetworkHost monitorNetworkHost) throws DnsException, DnsNotFoundException, DnsIllegalArgumentException {

    MonitorReport monitorReport = new MonitorReport("Check Host (Ptr)");
    String hostname = monitorNetworkHost.getName();
    DnsName hostDnsName = this.dnsSession.createDnsName(hostname);

    /**
     * Ipv4 PTR check
     */
    DnsIp dnsIpv4Address = hostDnsName
      .getFirstDnsIpv4Address();

    if (dnsIpv4Address.getInetAddress().equals(monitorNetworkHost.getIpv4())) {
      monitorReport.addSuccess("The host (" + monitorNetworkHost + ") has the ipv4 (" + monitorNetworkHost.getIpv4().getHostAddress() + ")");
    } else {
      monitorReport.addFailure("The host (" + monitorNetworkHost + ") has NOT the ipv4 (" + monitorNetworkHost.getIpv4().getHostAddress() + ")");
    }

    try {
      DnsName ptrName = dnsIpv4Address.getReverseDnsName();
      if (ptrName.equals(hostDnsName)) {
        monitorReport.addSuccess("The ipv4 (" + dnsIpv4Address + ") has the reverse PTR name (" + hostDnsName + ")");
      } else {
        monitorReport.addSuccess("The ipv4 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
      }
    } catch (DnsNotFoundException e) {
      monitorReport.addFailure("The ipv4 PTR for the host (" + monitorNetworkHost + ") was not found");
    }

    /**
     * Ipv6 PTR check
     */
    DnsIp dnsIpv6Address = hostDnsName
      .getFirstDnsIpv6Address();

    String hostIpv6Address = monitorNetworkHost.getIpv6().getHostAddress();
    if (dnsIpv6Address.getInetAddress().equals(monitorNetworkHost.getIpv6())) {
      monitorReport.addSuccess("The host (" + monitorNetworkHost + ") has the ipv6 (" + hostIpv6Address + ")");
    } else {
      monitorReport.addFailure("The host (" + monitorNetworkHost + ") has NOT the ipv6 (" + hostIpv6Address + ")");
    }

    try {
      DnsName ptrName = dnsIpv6Address.getReverseDnsName();
      if (ptrName.equals(hostDnsName)) {
        monitorReport.addSuccess("The ipv6 (" + dnsIpv6Address + ") has the reverse PTR name (" + hostDnsName + ")");
      } else {
        monitorReport.addSuccess("The ipv6 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
      }
    } catch (DnsNotFoundException e) {
      monitorReport.addFailure("The ipv6 PTR for the host (" + monitorNetworkHost + ") was not found");
    }

    return monitorReport;
  }


  /**
   * @param mainSpfDomain - the name of the domain where the original spf value is stored
   * @param thirdDomains  - the name of domains that should include the original spf records
   * @param mailerHost    - the mailer host
   */
  public List<MonitorReport> checkSpf(String mainSpfDomain, List<String> thirdDomains, MonitorNetworkHost mailerHost) throws DnsIllegalArgumentException, DnsException {

    List<MonitorReport> monitorReports = new ArrayList<>();
    /**
     * Check the spf value.
     * The spf record is stored in a subdomain and included
     * everywhere after that.
     */
    MonitorReport monitorReport = new MonitorReport("Main Spf Check");
    monitorReports.add(monitorReport);
    String mailjetSpf = "spf.mailjet.com";
    String googleSpf = "_spf.google.com";
    String mailChimpSpf = "spf.mandrillapp.com";
    String forwardMailSpf = "spf.forwardemail.net";
    String expectedFullSpfRecord = "v=spf1 mx ip4:" + mailerHost.getIpv4().getHostAddress() + "/32 ip6:" + mailerHost.getIpv6().getHostAddress() + " include:" + mailjetSpf + " include:" + googleSpf + " include:" + mailChimpSpf + " include:" + forwardMailSpf + " -all";
    DnsName spfMainDomainName = dnsSession.createDnsName(mainSpfDomain);
    DnsName spfSubDomainName = spfMainDomainName.getSubdomain("spf");
    checkSpfRecordForDomain(expectedFullSpfRecord, spfSubDomainName, monitorReport);

    /**
     * Check the spf include record in all domains
     */
    MonitorReport monitorReportDomainChecks = new MonitorReport("Domains Spf Check");
    monitorReports.add(monitorReportDomainChecks);
    String expectedIncludeSpfRecord = "v=spf1 include:" + spfSubDomainName.getNameWithoutRoot() + " -all";
    Set<DnsName> domainToChecks = new HashSet<>();
    domainToChecks.add(spfMainDomainName);
    for (String domainName : thirdDomains) {
      domainToChecks.add(this.dnsSession.createDnsName(domainName));
    }
    for (DnsName dnsName : domainToChecks) {
      checkSpfRecordForDomain(expectedIncludeSpfRecord, dnsName, monitorReportDomainChecks);
    }

    return monitorReports;

  }

  private void checkSpfRecordForDomain(String expectedIncludeSpfRecord, DnsName dnsName, MonitorReport monitorReport) throws DnsException {
    try {
      String spfRecordValue = dnsName.getSpfRecord();
      if (spfRecordValue.equals(expectedIncludeSpfRecord)) {
        monitorReport.addSuccess("The spf record has the good value in the domain (" + dnsName + ")");
      } else {
        monitorReport.addFailure("The spf record has not the good value in the domain (" + dnsName + ")\n. " +
          "  - The value is: " + spfRecordValue + "\n" +
          "  - The value should be: " + expectedIncludeSpfRecord
        );
      }
    } catch (DnsNotFoundException e) {
      monitorReport.addFailure("The spf record was not found for the domain (" + dnsName + ")");
    }
  }


  /**
   * Verification of DKIM value
   * <p>
   * Note that the ansible postfix role
   * has a real verification
   * with the task `Verification of the DNS Zone against the private key`
   * that starts
   * ```
   * ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags dkim-test-key -v
   * ```
   */
  public void dkimCombostrapTest() throws IOException {

    String combostrapDkimSelector = "combostrap";
    String combostrapExpectedDkimValue = "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDQUtHxTD63yxwq5fmjJ3RtXw2NP5/QEiSq3Xx85faTHnnj3/PA/igwWaueDsoeUuZOpkL74gDNGWBoQPecRaFrAXdPoEKGDYNBeMXzIkWQOth9Oaq4N+38LV08Ui86so8B2BhcvgXiqpACsrPur0hbDQWI183tZve7MKMPs3KPIQIDAQAB";
    this.testDkimUtil(combostrapDkimSelector, combostrapExpectedDkimValue, true);

  }

  /**
   * Google Dkim should be present on all domain
   */
  public void dkimGoogleTest() throws IOException {

    String googleDkimSelector = "google";
    String googleDkimValue = "v=DKIM1;k=rsa;p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm+CwENjyJ8AuCzA0HSW3E81VGZEr0wtdUkkZeOFuT7jUNFW+GMHZ8aHzFAHSn3R3aPyt7zDdsHJjXpBmqd9Eh0Lsf620h820myxvARO7Zgpwn7R+l/WHURzBKGIERvlvSRF/32YJ8oEkVPe/SYwDuobleusSCariqwbD8FHnIi4UJSnAasAiFIGIGzxzMm oObeRq5cCV2FMtstyJvtfF6LWJjL2k5020HbYaC0waT3lvSSbgW2PhSk0fX099U5Ij4Lmwb0oGdlYtVWXl6VaSWZHZXBBvdX2g+tKeHwYrVUck04hvTP7nsKOq+NTM+r+TQht3q425dGZYKOdUwZxJhQIDAQAB;t=s;";
    this.testDkimUtil(googleDkimSelector, googleDkimValue, false);

  }

  private void testDkimUtil(String dkimSelector, String expectedDkimValue, boolean checkValue) throws IOException {

    //for (String domain : DOMAINS) {
//      DnsDomain dnsDomain = DnsDomain.createFrom(domain);
//      TXTRecord domainDkimTextRecord = dnsDomain.getDkimRecord(dkimSelector);
//      //Assert.assertNotNull("The dkim record for the name (" + dnsDomain.getDkimName(dkimSelector) + ") was not found", domainDkimTextRecord);
//      if (checkValue) {
//        String actualDkimStringValue = DnsUtil.getStringFromTxtRecord(domainDkimTextRecord);
//        //Assert.assertEquals("The dkim record for the name (" + dnsDomain.getDkimName(dkimSelector) + ") should be good", expectedDkimValue, actualDkimStringValue);
//      }
    //}

  }


  public void check() throws ExecutionException, InterruptedException, TextParseException {
    DnsDomainData combostrap = DnsDomainData.create("combostrap.com");

    LookupSession lookupSession = LookupSession.builder()
      .resolver(DnsResolver.getLocal())
      .cache(new Cache())
      .build();
    List<DnsDomainData> domains = new ArrayList<>();
    domains.add(combostrap);
    for (DnsDomainData domain : domains) {

      LookupResult lookupResult = lookupSession.lookupAsync(domain.getName(), Type.TXT)
        .toCompletableFuture()
        .get();
      for (Record record : lookupResult.getRecords()) {
        TXTRecord txtRecord = ((TXTRecord) record);
        System.out.println(DnsUtil.getStringFromTxtRecord(txtRecord));
      }
//      DnsDomain dnsDomain = DnsDomain.createFrom(domain);
//      Name name = dnsDomain.getDmarcName();
//      TXTRecord dmarcTextRecord = dnsDomain.getDmarcRecord();
//      Assert.assertNotNull("The dmarc record for the name (" + name + ") was not found", dmarcTextRecord);
//      String actualDmarcStringValue = DnsUtil.getStringFromTxtRecord(dmarcTextRecord);
//      Assert.assertEquals("The dmarc record for the name (" + name + ") should be good", expectedDMarcValue, actualDmarcStringValue);
    }
  }


}
