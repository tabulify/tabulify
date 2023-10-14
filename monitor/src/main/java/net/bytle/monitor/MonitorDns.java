package net.bytle.monitor;

import net.bytle.dns.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.MXRecord;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * To finish it, see
 * <a href="https://support.google.com/mail/answer/81126">...</a>
 */
public class MonitorDns {

  public static Logger LOGGER = LogManager.getLogger(MonitorMain.class);

  private final DnsSession dnsSession;
  private final Map<DnsName, List<MonitorReportResult>> reports;

  public MonitorDns() {

    this.dnsSession = DnsSession.builder()
      // we are at cloudflare, no need to wait
      .setResolverToCloudflare()
      .build();
    this.reports = new HashMap<>();

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

  public MonitorDns checkMailersPtr(List<DnsHost> mailers) throws DnsException {

    String checkName = "Check Mailer Host (Ptr)";
    for (DnsHost dnsHost : mailers) {
      DnsName hostDnsName = dnsHost.getDnsName();
      /**
       * Ipv4 PTR check
       */
      DnsIp dnsIpv4Address;
      try {
        dnsIpv4Address = hostDnsName.getFirstDnsIpv4Address();
      } catch (DnsNotFoundException e) {
        this.addFailure(checkName, hostDnsName, e.getMessage());
        continue;
      }

      if (dnsIpv4Address.equals(dnsHost.getIpv4())) {
        this.addSuccess(checkName, hostDnsName, "The host (" + dnsHost + ") has the ipv4 (" + dnsHost.getIpv4() + ")");
      } else {
        this.addFailure(checkName, hostDnsName, "The host (" + dnsHost + ") has NOT the ipv4 (" + dnsHost.getIpv4() + ")");
      }

      try {
        DnsName ptrName = dnsIpv4Address.getReverseDnsName();
        if (ptrName.equals(hostDnsName)) {
          this.addSuccess(checkName, hostDnsName, "The ipv4 (" + dnsIpv4Address + ") has the reverse PTR name (" + hostDnsName + ")");
        } else {
          this.addSuccess(checkName, hostDnsName, "The ipv4 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
        }
      } catch (DnsNotFoundException e) {
        this.addFailure(checkName, hostDnsName, "The ipv4 PTR for the host (" + dnsHost + ") was not found");
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
          this.addFailure(checkName, hostDnsName, e.getMessage());
          continue;
        }
        if (dnsIpv6Address.equals(ipv6)) {
          this.addSuccess(checkName, hostDnsName, "The host (" + dnsHost + ") has the ipv6 (" + hostIpv6Address + ")");
        } else {
          this.addFailure(checkName, hostDnsName, "The host (" + dnsHost + ") has NOT the ipv6 (" + hostIpv6Address + ")");
        }

        try {
          DnsName ptrName = dnsIpv6Address.getReverseDnsName();
          if (ptrName.equals(hostDnsName)) {
            this.addSuccess(checkName, hostDnsName, "The ipv6 (" + dnsIpv6Address + ") has the reverse PTR name (" + hostDnsName + ")");
          } else {
            this.addSuccess(checkName, hostDnsName, "The ipv6 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
          }
        } catch (DnsNotFoundException e) {
          this.addFailure(checkName, hostDnsName, "The ipv6 PTR for the host (" + dnsHost + ") was not found");
        }

      }
    }


    return this;
  }

  private void addSuccess(String checkName, DnsName dnsName, String message) {
    this.addMessage(checkName, dnsName, message, MonitorReportResultStatus.SUCCESS);
  }

  private void addMessage(String checkName, DnsName dnsName, String message, MonitorReportResultStatus success) {
    MonitorReportResult monitorReportResult = MonitorReportResult.create(success, message).setCheckName(checkName);
    DnsName apexName = dnsName.getApexName();
    List<MonitorReportResult> values = this.reports.computeIfAbsent(apexName, k -> new ArrayList<>());
    values.add(monitorReportResult);
  }


  /**
   * @param mainDomain   - the name of the domain where the original spf value is stored
   * @param thirdDomains - the name of domains that should include the original spf records
   * @param mailersName  - the name where the A record name of the mailers are stored
   */
  public MonitorDns checkSpf(DnsName mainDomain, DnsName mailersName, Set<DnsName> thirdDomains) throws DnsIllegalArgumentException {


    DnsName spfSubDomainName = mainDomain.getSubdomain("spf");

    /**
     * Check the spf value.
     * The spf record is stored in a subdomain and included
     * everywhere after that.
     */
    String checkName = "Main Spf Check";
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
    checkSpfRecordForDomain(expectedFullSpfRecord, spfSubDomainName, checkName);

    /**
     * Check the spf include record in all domains
     */
    checkName = "Domains Spf Check";
    String expectedIncludeSpfRecord = "v=spf1 include:" + spfSubDomainName.getNameWithoutRoot() + " -all";
    checkSpfRecordForDomain(expectedIncludeSpfRecord, mainDomain, checkName);
    for (DnsName dnsName : thirdDomains) {
      checkSpfRecordForDomain(expectedIncludeSpfRecord, dnsName, checkName);
    }

    return this;

  }

  private void checkSpfRecordForDomain(String expectedIncludeSpfRecord, DnsName dnsName, String checkName) {
    String spfRecordValue;
    try {
      spfRecordValue = dnsName.getSpfRecord();
    } catch (DnsNotFoundException e) {
      this.addFailure(checkName, dnsName, "The spf record was not found for the domain (" + dnsName + ")");
      return;
    } catch (DnsException e) {
      this.addFailure(checkName, dnsName, "The spf record was not found for the domain (" + dnsName + ") due to a network problem: " + e.getMessage());
      return;
    }
    if (spfRecordValue.equals(expectedIncludeSpfRecord)) {
      this.addSuccess(checkName, dnsName, "The spf record has the good value in the domain (" + dnsName + ")");
    } else {
      this.addFailure(checkName, dnsName, "The spf record has not the good value in the domain (" + dnsName + ")\n. " +
        "  - The value is: " + spfRecordValue + "\n" +
        "  - The value should be: " + expectedIncludeSpfRecord
      );
    }

  }

  private void addFailure(String checkName, DnsName dnsName, String message) {
    this.addMessage(checkName, dnsName, message, MonitorReportResultStatus.FAILURE);
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

  public MonitorDns checkMx(Map<String, Integer> mxs, Set<DnsName> domains) {
    String mxCheck = "Mx Check";

    for (DnsName domain : domains) {
      List<MXRecord> mxRecords;
      try {
        mxRecords = domain.getMxRecords();
      } catch (DnsException e) {
        this.addFailure(mxCheck, domain, "A DNS exception has occurred: " + e.getMessage());
        continue;
      } catch (DnsNotFoundException e) {
        this.addFailure(mxCheck, domain, "The mx records were not found");
        continue;
      }

      if (mxs.size() != mxRecords.size()) {
        this.addFailure(mxCheck, domain, "The expected number of mx is (" + mxs.size() + ") but there are (" + mxRecords.size() + ")");
      }
      Map<String, Integer> actualMxs = new HashMap<>();
      for (MXRecord mxRecord : mxRecords) {
        String target = mxRecord.getTarget().toString(true);
        actualMxs.put(target, mxRecord.getPriority());
      }
      for (Map.Entry<String, Integer> expectedMx : mxs.entrySet()) {
        String expectedMxHost = expectedMx.getKey();
        Integer actualMxPriority = actualMxs.get(expectedMxHost);
        actualMxs.remove(expectedMxHost);
        if (actualMxPriority == null) {
          this.addFailure(mxCheck, domain, "The mx (" + expectedMxHost + ") was not found");
          continue;
        }
        Integer expectedPriority = expectedMx.getValue();
        if (!actualMxPriority.equals(expectedPriority)) {
          this.addFailure(mxCheck, domain, "The mx (" + expectedMxHost + ") has the priority (" + actualMxPriority + ") but should have (" + expectedPriority + ")");
          continue;
        }
        this.addSuccess(mxCheck, domain, "The mx (" + expectedMx + ") was found with the good priority (" + expectedPriority + ")");
      }
      for (String actualMx : actualMxs.keySet()) {
        this.addFailure(mxCheck, domain, "The mx (" + actualMx + ") was not expected and should be deleted.");
      }
    }
    return this;
  }

  public MonitorDns checkARecord(DnsHost host, Set<DnsName> domains, String checkTitle) {

    for (DnsName dnsName : domains) {
      try {
        DnsIp dnsIp = dnsName.getFirstARecord();
        if (dnsIp.equals(host.getIpv4())) {
          this.addSuccess(checkTitle, dnsName, "The ipv4 address (" + dnsIp + ") of the domain (" + dnsName + ") is the expected one for the host (" + host + ")");
        } else {
          this.addFailure(checkTitle, dnsName, "The ipv4 address (" + dnsIp + ") of the domain (" + dnsName + ") is not the expected ipv4 (" + host.getIpv4() + ") of the host (" + host + ")");
        }
      } catch (DnsNotFoundException | DnsException e) {
        this.addFailure(checkTitle, dnsName, e.getMessage());
      }
    }
    return this;
  }

  public List<MonitorReport> checkAll() throws UnknownHostException, DnsException, DnsIllegalArgumentException {


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

    DnsHost monitorOegHost = this.dnsSession.configHost("oeg.bytle.net")
      .setIpv4("143.176.206.82")
      .build();

    DnsName eraldyDomain = this.dnsSession.createDnsName("eraldy.com");
    DnsName mailersName = eraldyDomain.getSubdomain("mailers");
    LOGGER.info("Monitor Check Mailers");
    this.checkMailersARecord(mailers, mailersName);
    this.checkMailersPtr(mailers);


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
    this.checkSpf(eraldyDomain, mailersName, domains);

    LOGGER.info("Monitor A record for HTTP website");
    this.checkARecord(monitorBeauHost, domains, "Monitor A record for HTTP website");

    LOGGER.info("Monitor A record for subdomain");
    DnsName oeg = gerardNicoDomain.getSubdomain("oeg");
    this.checkARecord(monitorOegHost, Set.of(oeg), "Monitor Subdomain A record");
    DnsName rixt = gerardNicoDomain.getSubdomain("rixt");
    this.checkARecord(monitorBeauHost, Set.of(rixt), "Monitor Subdomain A record");

    LOGGER.info("Monitor Check Mx");
    Map<String, Integer> mxs = new HashMap<>();
    mxs.put("aspmx.l.google.com", 1);
    mxs.put("alt1.aspmx.l.google.com", 5);
    mxs.put("alt2.aspmx.l.google.com", 5);
    mxs.put("alt3.aspmx.l.google.com", 10);
    mxs.put("alt4.aspmx.l.google.com", 10);
    this.checkMx(mxs, domains);

    LOGGER.info("Monitor Check Dmarc");
    this.checkDmarc(domains);


    LOGGER.info("Monitor Creating report");
    List<MonitorReport> monitorReports = new ArrayList<>();
    for (DnsName dnsName : reports.keySet()) {
      MonitorReport monitorReport = new MonitorReport("Dns check for " + dnsName);
      monitorReports.add(monitorReport);
      for (MonitorReportResult monitorReportResult : reports.get(dnsName)) {
        monitorReport.addResult(monitorReportResult);
      }
    }
    return monitorReports;

  }

  private MonitorDns checkDmarc(Set<DnsName> domains) {
    String checkName = "Dmarc check";
    String expectedDmarc = "v=DMARC1; p=none; rua=mailto:dmarc@combostrap.com";
    for (DnsName domain : domains) {
      try {
        String dmarc = domain.getDmarcRecord();
        if (dmarc.equals(expectedDmarc)) {
          this.addSuccess(checkName, domain, "The dmarc is correct");
        } else {
          this.addSuccess(checkName, domain, "The dmarc is incorrect. It should be (" + expectedDmarc + ")");
        }
      } catch (DnsException e) {
        this.addFailure(checkName, domain, "The dmarc record query fires an exception: " + e.getMessage());
      } catch (DnsNotFoundException e) {
        this.addFailure(checkName, domain, "The dmarc record was not found");
      }
    }
    return this;
  }


}
