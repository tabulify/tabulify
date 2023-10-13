package net.bytle.monitor;

import net.bytle.dns.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * To finish it, see
 * <a href="https://support.google.com/mail/answer/81126">...</a>
 */
public class MonitorDns {

  public static Logger LOGGER = LogManager.getLogger(MonitorMain.class);

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

  public MonitorReport checkMailersPtr(List<DnsHost> mailers) throws DnsException, DnsIllegalArgumentException {

    MonitorReport monitorReport = new MonitorReport("Check Host (Ptr)");
    for (DnsHost dnsHost : mailers) {
      String hostname = dnsHost.getName();
      DnsName hostDnsName = this.dnsSession.createDnsName(hostname);

      /**
       * Ipv4 PTR check
       */
      DnsIp dnsIpv4Address;
      try {
        dnsIpv4Address = hostDnsName.getFirstDnsIpv4Address();
      } catch (DnsNotFoundException e) {
        monitorReport.addFailure(e.getMessage());
        continue;
      }

      if (dnsIpv4Address.equals(dnsHost.getIpv4())) {
        monitorReport.addSuccess("The host (" + dnsHost + ") has the ipv4 (" + dnsHost.getIpv4() + ")");
      } else {
        monitorReport.addFailure("The host (" + dnsHost + ") has NOT the ipv4 (" + dnsHost.getIpv4() + ")");
      }

      try {
        DnsName ptrName = dnsIpv4Address.getReverseDnsName();
        if (ptrName.equals(hostDnsName)) {
          monitorReport.addSuccess("The ipv4 (" + dnsIpv4Address + ") has the reverse PTR name (" + hostDnsName + ")");
        } else {
          monitorReport.addSuccess("The ipv4 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
        }
      } catch (DnsNotFoundException e) {
        monitorReport.addFailure("The ipv4 PTR for the host (" + dnsHost + ") was not found");
      }

      /**
       * Ipv6 PTR check
       */
      DnsIp ipv6 = dnsHost.getIpv6();
      if (ipv6 != null) {

        String hostIpv6Address = ipv6.getAddress();
        DnsIp dnsIpv6Address;
        try {
          dnsIpv6Address = hostDnsName.getFirstDnsIpv6Address();
        } catch (DnsNotFoundException e) {
          monitorReport.addFailure(e.getMessage());
          continue;
        }
        if (dnsIpv6Address.equals(ipv6)) {
          monitorReport.addSuccess("The host (" + dnsHost + ") has the ipv6 (" + hostIpv6Address + ")");
        } else {
          monitorReport.addFailure("The host (" + dnsHost + ") has NOT the ipv6 (" + hostIpv6Address + ")");
        }

        try {
          DnsName ptrName = dnsIpv6Address.getReverseDnsName();
          if (ptrName.equals(hostDnsName)) {
            monitorReport.addSuccess("The ipv6 (" + dnsIpv6Address + ") has the reverse PTR name (" + hostDnsName + ")");
          } else {
            monitorReport.addSuccess("The ipv6 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
          }
        } catch (DnsNotFoundException e) {
          monitorReport.addFailure("The ipv6 PTR for the host (" + dnsHost + ") was not found");
        }

      }
    }


    return monitorReport;
  }


  /**
   * @param mainDomain   - the name of the domain where the original spf value is stored
   * @param thirdDomains - the name of domains that should include the original spf records
   * @param mailersName  - the name where the A record name of the mailers are stored
   */
  public List<MonitorReport> checkSpf(DnsName mainDomain, DnsName mailersName, Set<DnsName> thirdDomains) throws DnsIllegalArgumentException {

    List<MonitorReport> monitorReports = new ArrayList<>();


    DnsName spfSubDomainName = mainDomain.getSubdomain("spf");

    /**
     * Check the spf value.
     * The spf record is stored in a subdomain and included
     * everywhere after that.
     */
    MonitorReport monitorReport = new MonitorReport("Main Spf Check");
    monitorReports.add(monitorReport);
    List<String> includes = Arrays.asList(
      //"spf.mailjet.com",
      "_spf.google.com",
      //"spf.mandrillapp.com",
      "spf.forwardemail.net"
    );
    String includeMechanism = " include:";
    String expectedFullSpfRecord = "v=spf1 mx a:" +
      mailersName.getNameWithoutRoot() +
      includeMechanism + String.join(includeMechanism, includes) +
      " -all";
    checkSpfRecordForDomain(expectedFullSpfRecord, spfSubDomainName, monitorReport);

    /**
     * Check the spf include record in all domains
     */
    MonitorReport monitorReportDomainChecks = new MonitorReport("Domains Spf Check");
    monitorReports.add(monitorReportDomainChecks);
    String expectedIncludeSpfRecord = "v=spf1 include:" + spfSubDomainName.getNameWithoutRoot() + " -all";

    checkSpfRecordForDomain(expectedIncludeSpfRecord, mainDomain, monitorReportDomainChecks);
    for (DnsName dnsName : thirdDomains) {
      checkSpfRecordForDomain(expectedIncludeSpfRecord, dnsName, monitorReportDomainChecks);
    }

    return monitorReports;

  }

  private void checkSpfRecordForDomain(String expectedIncludeSpfRecord, DnsName dnsName, MonitorReport monitorReport) {
    String spfRecordValue;
    try {
      spfRecordValue = dnsName.getSpfRecord();
    } catch (DnsNotFoundException e) {
      monitorReport.addFailure("The spf record was not found for the domain (" + dnsName + ")");
      return;
    } catch (DnsException e) {
      monitorReport.addFailure("The spf record was not found for the domain (" + dnsName + ") due to a network problem: " + e.getMessage());
      return;
    }
    if (spfRecordValue.equals(expectedIncludeSpfRecord)) {
      monitorReport.addSuccess("The spf record has the good value in the domain (" + dnsName + ")");
    } else {
      monitorReport.addFailure("The spf record has not the good value in the domain (" + dnsName + ")\n. " +
        "  - The value is: " + spfRecordValue + "\n" +
        "  - The value should be: " + expectedIncludeSpfRecord
      );
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


  public MonitorReport checkMailersARecord(List<DnsHost> mailers, DnsName mailersName) {


    MonitorReport monitorReport = new MonitorReport("Check Mailers A record");

    try {
      Set<DnsIp> aIps;
      try {
        aIps = mailersName.getARecords();
      } catch (DnsNotFoundException e) {
        aIps = new HashSet<>();
      }
      for (DnsHost dnsHost : mailers) {
        if (aIps.contains(dnsHost.getIpv4())) {
          monitorReport.addSuccess("The mailer host (" + dnsHost + ") ip address (" + dnsHost.getIpv4() + ") was found in the name (" + mailersName + ")");
        } else {
          monitorReport.addFailure("The mailer host (" + dnsHost + ") ip address (" + dnsHost.getIpv4() + ") was NOT found in the name (" + mailersName + ")");
        }
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    }
    return monitorReport;

  }

  public MonitorReport checkMx(Map<String, Integer> mxs, Set<DnsName> domains) {
    MonitorReport monitorReport = new MonitorReport("Mx Check");
    monitorReport.addFailure("Not done");
    return monitorReport;
  }

  public MonitorReport checkARecord(DnsHost host, Set<DnsName> domains) {
    MonitorReport monitorReport = new MonitorReport("HTTP A record Check");
    for (DnsName dnsName : domains) {

      try {
        DnsIp dnsIp = dnsName.getFirstARecord();
        if (dnsIp.equals(host.getIpv4())) {
          monitorReport.addSuccess("The ipv4 address (" + dnsIp + ") of the domain (" + dnsName + ") is not the expected one for the host (" + host + ")");
        } else {
          monitorReport.addFailure("The ipv4 address (" + dnsIp + ") of the domain (" + dnsName + ") is not the expected ipv4 (" + host.getIpv4() + ") of the host (" + host + ")");
        }
      } catch (DnsNotFoundException | DnsException e) {
        monitorReport.addFailure(e.getMessage());
      }

    }
    return monitorReport;
  }

  public List<MonitorReport> checkAll() throws UnknownHostException, DnsException, DnsIllegalArgumentException {

    List<MonitorReport> monitorReports = new ArrayList<>();
    LOGGER.info("Monitor Check Host");
    DnsHost monitorBeauHost = this.dnsSession.configHost("beau.bytle.net")
      .setIpv4("192.99.55.226")
      .setIpv6("2607:5300:201:3100::85b")
      .build();
    DnsHost monitorCanaHost = this.dnsSession.configHost("cana.bytle.net")
      .setIpv4("51.79.86.27")
      .build();
    List<DnsHost> mailers = new ArrayList<>();
    mailers.add(monitorBeauHost);
    mailers.add(monitorCanaHost);

    @SuppressWarnings("unused") DnsHost monitorOegHost = this.dnsSession.configHost("oeg.bytle.net")
      .setIpv4("143.176.206.82")
      .build();

    DnsName eraldyDomain = this.dnsSession.createDnsName("eraldy.com");
    DnsName mailersName = eraldyDomain.getSubdomain("mailers");
    LOGGER.info("Monitor Check Mailers");
    monitorReports.add(this.checkMailersARecord(mailers, mailersName));
    monitorReports.add(this.checkMailersPtr(mailers));


    LOGGER.info("Monitor Check Domain Spf");
    DnsName gerardNicoDomain = this.dnsSession.createDnsName("gerardnico.com");
    Set<DnsName> domains = Set.of(
      this.dnsSession.createDnsName("bytle.net"),
      this.dnsSession.createDnsName("combostrap.com"),
      this.dnsSession.createDnsName("datacadamia.com"),
      eraldyDomain,
      gerardNicoDomain,
      this.dnsSession.createDnsName("persso.com"),
      this.dnsSession.createDnsName("tabulify.com")
    );
    monitorReports.addAll(this.checkSpf(eraldyDomain, mailersName, domains));

    LOGGER.info("Monitor A record for HTTP website");
    monitorReports.add(this.checkARecord(monitorBeauHost, domains));

    LOGGER.info("Monitor A record for Home");
    monitorReports.add(this.checkARecord(monitorOegHost, gerardNicoDomain.getSubdomain("oeg")));

    LOGGER.info("Monitor Check Mx");
    Map<String, Integer> mxs = new HashMap<>();
    mxs.put("aspmx.l.google.com", 1);
    mxs.put("alt1.aspmx.l.google.com", 5);
    mxs.put("alt2.aspmx.l.google.com", 5);
    mxs.put("alt3.aspmx.l.google.com", 10);
    mxs.put("alt4.aspmx.l.google.com", 10);
    monitorReports.add(this.checkMx(mxs, domains));

    return monitorReports;

  }

  private MonitorReport checkARecord(DnsHost monitorOegHost, DnsName oeg) {

    return null;
  }


}
