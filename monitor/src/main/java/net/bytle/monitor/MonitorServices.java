package net.bytle.monitor;

import io.vertx.core.Future;
import io.vertx.core.net.NetClient;
import net.bytle.dns.*;
import net.bytle.exception.CastException;
import net.bytle.type.DnsName;
import net.bytle.type.EmailAddress;
import net.bytle.vertx.ConfigAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Monitor Services (DNS name, Certificates)
 * To finish it, see
 * <a href="https://support.google.com/mail/answer/81126">...</a>
 */
public class MonitorServices {

  public static Logger LOGGER = LogManager.getLogger(MonitorMain.class);

  private final Map<DnsName, List<MonitorReportResult>> reports;
  private final CloudflareDns cloudflareDns;

  private final DnsHost monitorBeauHost;
  private final DnsHost monitorOegHost;
  private final ArrayList<DnsHost> mailers;
  private final Map<DnsName, List<Future<MonitorReportResult>>> asyncResults = new HashMap<>();
  private final DnsName eraldyDomain;
  private final DnsName gerardNicoDomain;
  private final Set<DnsName> apexDomains;
  private final Map<String, Integer> apexMxs;
  private final Map<String, Integer> eraldyInboxMx;
  private final XBillDnsClient dnsClient;
  private final NetClient netClient;
  private final long certificateExpirationDelayBeforeFailure;
  private final HashMap<DnsName, Future<List<MonitorReportResult>>> asyncListResults = new HashMap<>();
  private final DnsHost eraldyComHost;
  private final DnsHost membersEraldyComHost;

  public MonitorServices(CloudflareDns cloudflareDns, NetClient sslNetClient, ConfigAccessor configAccessor) {

    /**
     * Because the check may run every week
     * The default is above 7 days
     */
    long defaultValue = 10L;
    this.certificateExpirationDelayBeforeFailure = configAccessor.getLong("certificate.expiration.delay.before.failure", defaultValue);
    LOGGER.info("Certificate expiration warning set to " + this.certificateExpirationDelayBeforeFailure);

    // we are at cloudflare, no need to wait the sync
    // we resolve to cloudflare immediately
    dnsClient = XBillDnsClient.builder()
      // we are at cloudflare, no need to wait the sync
      // we resolve to cloudflare immediately
      .setResolverToCloudflare()
      .build();
    this.reports = new HashMap<>();
    this.cloudflareDns = cloudflareDns;
    this.netClient = sslNetClient;

    /**
     * Host
     */
    LOGGER.info("Monitor Dns - Creating Hosts");
    try {
      monitorBeauHost = dnsClient.configHost("beau.bytle.net")
        .setIpv4("192.99.55.226")
        .setIpv6("2607:5300:201:3100::85b") // 2607:5300:201:3100:0:0:0:85b
        .build();

      eraldyComHost = dnsClient.configHost("eraldy.com")
        .setIpv4("66.241.125.178")
        .setIpv6("2a09:8280:1::36:4fc9:0")
        .build();
      membersEraldyComHost = dnsClient.configHost("members.eraldy.com")
        .setIpv4("66.241.125.170")
        .setIpv6("2a09:8280:1::36:52ab:0")
        .build();

      mailers = new ArrayList<>();
      mailers.add(monitorBeauHost);

      monitorOegHost = dnsClient.configHost("oeg.bytle.net")
        .setIpv4("143.176.206.82")
        .build();

      /**
       * Domains
       */
      LOGGER.info("Monitor Dns - Creating apex domains");
      eraldyDomain = DnsName.create("eraldy.com");
      gerardNicoDomain = DnsName.create("gerardnico.com");
      DnsName bytleDomain = DnsName.create("bytle.net");
      DnsName combostrapDomain = DnsName.create("combostrap.com");
      DnsName datacadamiaDomain = DnsName.create("datacadamia.com");
      DnsName tabulifyDomain = DnsName.create("tabulify.com");
      apexDomains = Set.of(
        bytleDomain,
        combostrapDomain,
        datacadamiaDomain,
        eraldyDomain,
        gerardNicoDomain,
        tabulifyDomain
      );
      LOGGER.info("Monitor DNS - Add Eraldy Dkim to domains");
      String eraldyDkimSelector = "eraldy";
      String combostrapExpectedDkimValue = "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDQUtHxTD63yxwq5fmjJ3RtXw2NP5/QEiSq3Xx85faTHnnj3/PA/igwWaueDsoeUuZOpkL74gDNGWBoQPecRaFrAXdPoEKGDYNBeMXzIkWQOth9Oaq4N+38LV08Ui86so8B2BhcvgXiqpACsrPur0hbDQWI183tZve7MKMPs3KPIQIDAQAB";
      for (DnsName dnsName : apexDomains) {
        dnsName.addExpectedDkim(eraldyDkimSelector, combostrapExpectedDkimValue);
      }
      LOGGER.info("Monitor DNS - Add Google Dkim to domains");
      // They are configuration of the Gmail app: https://admin.google.com/ac/apps/gmail/authenticateemail
      bytleDomain.addExpectedDkim("google", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgfxRVmzWeaZZYiF0jlvHV9fYn7piegwkvzj/N3HdTzeKwucVenLP6q5IfwqsO1C6aZGNhproVUQoyN+gDa5kfnLkiF22shXLeyG2lGtSje+xvQ4IppsA1mdXltDjclA3ipOjT45PZ83Rt421qEecHXmNFIk0gv4xPqQeCX6E3AJos3TC5zc/wJO7q3uyv9udtZuuEcZg2+2N3tTU4rNobWmIfzpvCO2LkQvFF8K+Szp9l4yU1Ji+6iTrJpJqbojj9HxsTt/SfsnUAqUROAbVVxpFtM/UAA/WLWIzOdIvDDL7mB7gTPzY17wtwvuDm+mtQGn4+LO5cSejnjRgO/hczwIDAQAB");
      combostrapDomain.addExpectedDkim("google", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm+CwENjyJ8AuCzA0HSW3E81VGZEr0wtdUkkZeOFuT7jUNFW+GMHZ8aHzFAHSn3R3aPyt7zDdsHJjXpBmqd9Eh0Lsf620h820myxvARO7Zgpwn7R+l/WHURzBKGIERvlvSRF/32YJ8oEkVPe/SYwDuobleusSCariqwbD8FHnIi4UJSnAasAiFIGIGzxzMmoObeRq5cCV2FMtstyJvtfF6LWJjL2k5020HbYaC0waT3lvSSbgW2PhSk0fX099U5Ij4Lmwb0oGdlYtVWXl6VaSWZHZXBBvdX2g+tKeHwYrVUck04hvTP7nsKOq+NTM+r+TQht3q425dGZYKOdUwZxJhQIDAQAB");
      datacadamiaDomain.addExpectedDkim("google", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkGtLDG3wTcwJIuWDiKLXIAB1VmIRKbt0G6RamA6I+vbJf9HmTcKnVtcGsTl5fNnOdBCpyZXn6Kq+swwmQXH91D0yjStlExS2hr56qIEcCm2lecKq98PdQtQA0BKE0bEna1xryiwlNCKtH2ZqipIwFHmZgTV3WPOkjqwNQxoj4m93EXZktfslUnP88v9ArHOZMpZgSbEDST7+h3XgoweBun0kId4YU6IFT/w/Cwnw4yt/xGPrL2KlRNrPI5VrrhkNkcAZg0RV1ESx/rT+ZWFQ5LHigwmy/VWmSZoKyQPHt3iFMjqB2wiTdgR0MGtNxSSV6mgHST3WORjMAN8UnQj0uwIDAQAB");
      eraldyDomain.addExpectedDkim("google", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnh121MFMGHy9ApWnneweFYrkGoNyJY2QL/QsH68qS0t4dYH9AEJZcnnLF4Bp1EPTvX7W6O9u9lrTEL3w+oAYHa74n9euN4dyPzyqN4WJYLiduXiX+JomHE1Y8QoR51EdP94hT9uNAGzQbX20JRlhkTCx4CdcTpPi3Uy7x77Ej+Rhkxxmd4HWpSOIizQorhN44SOODhNw75aM+Sw9TPcgmKxnrNZY73ZHUjJLxIBGgVwB2Cjt7i//UpX+k2f8cOkLEngSuUo8tQ9jTETpKhaor4Amo2SrsPz4kk+aZR3S/W0XV9P7MPy/6hNYYhQfZ+ifAGZnNlBzuOa59BeiRIL8OQIDAQAB");
      gerardNicoDomain.addExpectedDkim("google", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAndKU0WYuA04TnVdkiSS+XCaCNRhqEEkGcsFpiCmv1YS5juUfvN3J/hUKD03usyxrM6cy1xMLgVd5SOsZKlyQ2vUHWULQMcJWqNtwpmy/kniguen5P+xtfMT43Xk3749rcBLI7WeDoUt7uDNUqOM7JCFfX/K8bKqDlxlOOEPOlpoBw9VXCE7wvpXkl1PduuJy1L0AbwqIbq2CRpyU5IuPLvcuHvBrPKU18o82B486Lb/bdRzscMH3hUia8aPmdgIBa7sqbX+xU3qfOsIUUceRGYipzQ/xJQKvACVgA/wwwMFVzwaCtA8lQIL3B5rs/Pp5rtHZW5kB5/LJhrSqOpxwrQIDAQAB");
      tabulifyDomain.addExpectedDkim("google", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuzMY3bJmB9x2F7kv9+uQlDcv6CUz1iqiSKTPsJUdaG6mQgirIUNF3Bz9r7GJWkUxZA780dNMDcpqdH58VKdZznBZW1DceTqaM2B3/pMJmwA3JhQZl3la96vQFqIf5FrLfS5nofrZxVCGaTTMeesoJf7X/f9GUWcCr3HEUPbkT8eQ7bsh8EGhtc/aJYS9aCF7kzfKNB7ArXQnNowPMN/qIH6lv/zQWmF85C9doKK00I+SeLOHJR04yRSbEmQ1kKx8KaxxVgfTShbt/Thqzwd0o7tp50vXmBW8or1MZfFQ4/geXCcC5U8LVm3RT4pI84M/ekWhzlDD+PfN/Yif3OqSfQIDAQAB");

      LOGGER.info("Monitor DNS - Add Dmarc to domains");
      try {
        bytleDomain.addExpectedDmarcEmail(EmailAddress.of("44801f5f73014104b1898b84a16eb826@dmarc-reports.cloudflare.net"));
        combostrapDomain.addExpectedDmarcEmail(EmailAddress.of("970a4434804f4e449ca040d51d4e4588@dmarc-reports.cloudflare.net"));
        EmailAddress dmarcInternal = EmailAddress.of("dmarc@eraldy.com");
        for (DnsName apexDomain : apexDomains) {
          apexDomain.addExpectedDmarcEmail(dmarcInternal);
        }
      } catch (CastException e) {
        throw new RuntimeException("The email are literal, it should not happen", e);
      }

      LOGGER.info("Monitor Mx for Apex Domains");
      apexMxs = new HashMap<>();
      apexMxs.put("aspmx.l.google.com", 1);
      apexMxs.put("alt1.aspmx.l.google.com", 5);
      apexMxs.put("alt2.aspmx.l.google.com", 5);
      apexMxs.put("alt3.aspmx.l.google.com", 10);
      apexMxs.put("alt4.aspmx.l.google.com", 10);

      LOGGER.info("Monitor Eraldy Inbox Mx");
      eraldyInboxMx = new HashMap<>();
      eraldyInboxMx.put(monitorBeauHost.getName(), 1);

    } catch (DnsException | CastException | IllegalArgumentException | DnsIllegalArgumentException e) {
      throw new RuntimeException(e);
    }

  }


  public static MonitorServices create(CloudflareDns cloudflareDns, NetClient netClient, ConfigAccessor configAccessor) {
    return new MonitorServices(cloudflareDns, netClient, configAccessor);
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

  public MonitorServices checkMailersPtr(List<DnsHost> mailers)  {

    String checkName = "Check Mailer Host (Ptr)";
    for (DnsHost dnsHost : mailers) {

      DnsName hostDnsName = dnsHost.getDnsName();

      try {
        /**
         * Ipv4 PTR check
         */
        DnsIp dnsIpv4Address;
        try {
          dnsIpv4Address = dnsClient.lookupIpv4(hostDnsName);
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
          DnsName ptrName = dnsClient.lookupPtr(dnsIpv4Address);
          if (ptrName.equals(hostDnsName)) {
            this.addSuccess(checkName, hostDnsName, "The ipv4 (" + dnsIpv4Address + ") has the reverse PTR name (" + hostDnsName + ")");
          } else {
            this.addFailure(checkName, hostDnsName, "The ipv4 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
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
            dnsIpv6Address = dnsClient.lookupIpv6(hostDnsName);
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
            DnsName ptrName = dnsClient.lookupPtr(dnsIpv6Address);
            if (ptrName.equals(hostDnsName)) {
              this.addSuccess(checkName, hostDnsName, "The ipv6 (" + dnsIpv6Address + ") has the reverse PTR name (" + hostDnsName + ")");
            } else {
              this.addSuccess(checkName, hostDnsName, "The ipv6 (" + dnsIpv4Address + ") does not have as reverse PTR name (" + hostDnsName + ") but (" + ptrName + ")");
            }
          } catch (DnsNotFoundException e) {
            this.addFailure(checkName, hostDnsName, "The ipv6 PTR for the host (" + dnsHost + ") was not found");
          }

        }
      } catch (DnsException e) {
        this.addFailure(checkName, hostDnsName, "Fatal Dns Exception "+e.getMessage());
      }
    }


    return this;
  }

  private void addSuccess(String checkName, DnsName dnsName, String message) {
    this.addMessage(checkName, dnsName, message, MonitorReportResultStatus.SUCCESS);
  }

  private void addMessage(String checkName, DnsName dnsName, String message, MonitorReportResultStatus status) {
    MonitorReportResult monitorReportResult = createResult(checkName, message, status);
    //DnsName apexName = dnsName.getApexName();
    List<MonitorReportResult> values = this.reports.computeIfAbsent(dnsName, k -> new ArrayList<>());
    values.add(monitorReportResult);
  }

  private void addAsyncResult(DnsName dnsName, Future<MonitorReportResult> monitorReportResultFuture) {
    //DnsName apexName = dnsName.getApexName();
    List<Future<MonitorReportResult>> values = this.asyncResults.computeIfAbsent(dnsName, k -> new ArrayList<>());
    values.add(monitorReportResultFuture);
  }

  private MonitorReportResult createResult(String checkName, String message, MonitorReportResultStatus status) {
    return MonitorReportResult.create(status, message)
      .setCheckName(checkName);
  }


  /**
   * @param mainDomain   - the name of the domain where the original spf value is stored
   * @param thirdDomains - the name of domains that should include the original spf records
   * @param mailersName  - the name where the A and AAAA record name of the mailers are stored
   */
  public MonitorServices checkSpf(DnsName mainDomain, DnsName mailersName, Set<DnsName> thirdDomains) throws CastException {


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
      mailersName.toStringWithoutRoot() +
      includeMechanism + String.join(includeMechanism, includes) +
      " -all";
    checkSpfRecordForDomain(expectedFullSpfRecord, spfSubDomainName, checkName);

    /**
     * Check the spf include record in all domains
     */
    checkName = "Domains Spf Check";
    String expectedIncludeSpfRecord = "v=spf1 include:" + spfSubDomainName.toStringWithoutRoot() + " -all";
    checkSpfRecordForDomain(expectedIncludeSpfRecord, mainDomain, checkName);
    for (DnsName dnsName : thirdDomains) {
      checkSpfRecordForDomain(expectedIncludeSpfRecord, dnsName, checkName);
    }

    return this;

  }

  private void checkSpfRecordForDomain(String expectedIncludeSpfRecord, DnsName dnsName, String checkName) {
    String spfRecordValue;
    try {
      spfRecordValue = dnsClient.lookupSpf(dnsName);
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


  public MonitorServices checkMailersARecord(List<DnsHost> mailers, DnsName mailersName) {

    try {
      Set<DnsIp> aIps;
      try {
        aIps = dnsClient.resolveA(mailersName);
      } catch (DnsNotFoundException e) {
        aIps = new HashSet<>();
      }
      String checkTitle = "check Mailer A record";
      for (DnsHost dnsHost : mailers) {
        if (aIps.contains(dnsHost.getIpv4())) {
          this.addSuccess(checkTitle, mailersName, "The mailer host (" + dnsHost + ") ip address (" + dnsHost.getIpv4() + ") was found in the name (" + mailersName + ")");
        } else {
          this.addFailure(checkTitle, mailersName, "The mailer host (" + dnsHost + ") ip address (" + dnsHost.getIpv4() + ") was NOT found in the name (" + mailersName + ")");
        }
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    }
    return this;

  }

  public MonitorServices checkMx(Map<String, Integer> mxs, Set<DnsName> domains) {
    String mxCheck = "Mx Check";

    for (DnsName domain : domains) {
      List<DnsMxRecord> mxRecords;
      try {
        mxRecords = dnsClient.resolveMx(domain);
      } catch (DnsException e) {
        this.addFailure(mxCheck, domain, "A DNS exception has occurred: " + e.getMessage());
        continue;
      } catch (DnsNotFoundException e) {
        this.addFailure(mxCheck, domain, "The mx records were not found for the domain "+domain);
        continue;
      }

      if (mxs.size() != mxRecords.size()) {
        this.addFailure(mxCheck, domain, "The expected number of mx is (" + mxs.size() + ") but there are (" + mxRecords.size() + ")");
      }
      Map<String, Integer> actualMxs = new HashMap<>();
      for (DnsMxRecord mxRecord : mxRecords) {
        String target = mxRecord.getTarget().toStringWithoutRoot();
        actualMxs.put(target, mxRecord.getPriority());
      }
      for (Map.Entry<String, Integer> expectedMx : mxs.entrySet()) {
        String expectedMxHost = expectedMx.getKey();
        Integer actualMxPriority = actualMxs.get(expectedMxHost);
        actualMxs.remove(expectedMxHost);
        if (actualMxPriority == null) {
          this.addFailure(mxCheck, domain, "The mx (" + expectedMxHost + ") was not found for the domain "+domain);
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

  /**
   * A record for web hosting are proxied
   * We need then to go through the cloudflare api
   * to get the real value
   */
  public MonitorServices checkCloudflareARecord(DnsHost host, Set<DnsName> domains, String checkTitle) {


    for (DnsName dnsName : domains) {

      String cloudflareName = dnsName.toStringWithoutRoot();
      Future<MonitorReportResult> result = this.cloudflareDns.getZone(dnsName.getApexName().toStringWithoutRoot())
        .compose(zone -> zone.getName(cloudflareName)
          .getFirstARecordOrNull()
          .compose(inetAddress -> {
              MonitorReportResult futureResult;
              if (inetAddress == null) {
                futureResult = this.createResult(checkTitle, "The A record (ipv4 address) of the domain (" + cloudflareName + ") was not found with the value " + host.getIpv4().getAddress(), MonitorReportResultStatus.FAILURE);
              } else {
                if (inetAddress.equals(host.getIpv4().getInetAddress())) {
                  futureResult = this.createResult(checkTitle, "The ipv4 address (" + inetAddress + ") of the domain (" + cloudflareName + ") is the expected one for the host (" + host + ")", MonitorReportResultStatus.SUCCESS);
                } else {
                  futureResult = this.createResult(checkTitle, "The ipv4 address (" + inetAddress + ") of the domain (" + cloudflareName + ") is not the expected ipv4 (" + host.getIpv4() + ") of the host (" + host + ")", MonitorReportResultStatus.FAILURE);
                }
              }
              return Future.succeededFuture(futureResult);
            },
            err -> Future.succeededFuture(this.createResult(checkTitle, err.getMessage(), MonitorReportResultStatus.FAILURE))
          ));
      this.addAsyncResult(dnsName, result);

    }
    return this;
  }


  public List<MonitorReport> checkAll() throws CastException {


    LOGGER.info("Monitor Check Services started");


    LOGGER.info("Monitor Check Mailers");
    /**
     * See also https://support.google.com/mail/answer/9981691
     */
    DnsName mailersName = eraldyDomain.getSubdomain("mailers");
    LOGGER.info("  * Check Mailers A record");
    this.checkMailersARecord(mailers, mailersName);
    this.checkMailersAAAARecord(mailers, mailersName);
    LOGGER.info("  * Check Mailers Ptr record");
    this.checkMailersPtr(mailers);

    LOGGER.info("Monitor Check Domain Email");
    LOGGER.info("  * Check Spf");
    this.checkSpf(eraldyDomain, mailersName, apexDomains);
    LOGGER.info("  * Check Mx");
    this.checkMx(apexMxs, apexDomains);
    LOGGER.info("  * Check Dmarc");
    this.checkDmarc(apexDomains);
    LOGGER.info("  * Check Dkim");
    this.checkDkim(apexDomains);

    LOGGER.info("Monitor Inbox Mx service");
    DnsName inboxSubdomain = eraldyDomain.getSubdomain("inbox");
    this.checkMx(eraldyInboxMx, Set.of(inboxSubdomain));

    LOGGER.info("Monitor HTTP services");
    LOGGER.info("  * Check A record");
    // We check the A record and not a CNAME record
    // Why ? gerardnico.github.io hosted domain does not have cname but A and AAAAA records
    Set<DnsName> httpBeauServices = new HashSet<>(apexDomains);
    httpBeauServices.remove(eraldyComHost.getDnsName());
    // the www format of apex domain
    Set<DnsName> wwwNames = new HashSet<>();
    for (DnsName dnsName : httpBeauServices) {
      wwwNames.add(dnsName.getSubdomain("www"));
    }
    httpBeauServices.addAll(wwwNames);
    httpBeauServices.add(gerardNicoDomain.getSubdomain("rixt"));

    this.checkCloudflareARecord(monitorBeauHost, httpBeauServices, "Beau Domains HTTP A record");
    HashSet<DnsName> eraldyDnsNames = new HashSet<>();
    eraldyDnsNames.add(eraldyComHost.getDnsName());
    eraldyDnsNames.add(eraldyComHost.getDnsName().getSubdomain("www"));
    this.checkCloudflareARecord(eraldyComHost, eraldyDnsNames, "eraldy.com HTTP A record");
    this.checkCloudflareARecord(membersEraldyComHost, "members.eraldy.com HTTP A record");
    LOGGER.info("  * Check HTTP Certificates");
    Set<DnsName> httpServices = new HashSet<>(httpBeauServices);
    httpServices.add(eraldyComHost.getDnsName());
    httpServices.add(membersEraldyComHost.getDnsName());
    this.checkHttpsCertificates(httpServices);

    LOGGER.info("Monitor Private Network");
    LOGGER.info("  * Check A record for home");
    DnsName oeg = gerardNicoDomain.getSubdomain("oeg");
    this.checkCloudflareARecord(monitorOegHost, Set.of(oeg), "Monitor Subdomain A record");


    return this.getMonitorReports();

  }

  private MonitorServices checkCloudflareARecord(DnsHost host, String message) {
    HashSet<DnsName> hashSetDnsNames = new HashSet<>();
    hashSetDnsNames.add(host.getDnsName());
    checkCloudflareARecord(host, hashSetDnsNames, message);
    return this;
  }

  private MonitorServices checkMailersAAAARecord(ArrayList<DnsHost> mailers, DnsName mailersName) {
    try {
      Set<DnsIp> aIps;
      try {
        aIps = dnsClient.resolveAAAA(mailersName);
      } catch (DnsNotFoundException e) {
        aIps = new HashSet<>();
      }
      String checkTitle = "check Mailer AAAA record";
      for (DnsHost dnsHost : mailers) {
        if (aIps.contains(dnsHost.getIpv6())) {
          this.addSuccess(checkTitle, mailersName, "The mailer host (" + dnsHost + ") ip address (" + dnsHost.getIpv6() + ") was found in the name (" + mailersName + ")");
        } else {
          this.addFailure(checkTitle, mailersName, "The mailer host (" + dnsHost + ") ip address (" + dnsHost.getIpv6() + ") was NOT found in the name (" + mailersName + ")");
        }
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  private void checkHttpsCertificates(Set<DnsName> dnsNames) {
    int port = 443;
    String checkTitle = "Http Certificates";
    for (DnsName dnsName : dnsNames) {
      String nameWithoutRoot = dnsName.toStringWithoutRoot();

      String service = nameWithoutRoot + ":" + port;

      Future<List<MonitorReportResult>> futureResults = this.netClient.connect(port, nameWithoutRoot)
        .compose(netSocket -> {

            List<MonitorReportResult> monitorReportResults = new ArrayList<>();
            List<Certificate> certs;
            try {
              certs = netSocket.peerCertificates();
            } catch (SSLPeerUnverifiedException e) {
              monitorReportResults.add(createResult(checkTitle, "SSLPeerUnverifiedException for the service (" + service + "). Message: " + e.getMessage(), MonitorReportResultStatus.FAILURE));
              return Future.succeededFuture(monitorReportResults);
            }
            if (certs == null) {
              monitorReportResults.add(createResult(checkTitle, "The connection to the service (" + service + ") is not SSL/TLS. No certificate were returned", MonitorReportResultStatus.FAILURE));
              return Future.succeededFuture(monitorReportResults);
            }

            for (Certificate cert : certs) {

              if (!(cert instanceof java.security.cert.X509Certificate)) {
                monitorReportResults.add(createResult(checkTitle, "The certificate type (" + cert.getType() + ") returned by the service (" + service + ") is not supported", MonitorReportResultStatus.FAILURE));
                continue;
              }

              X509Certificate x509Certificate = (X509Certificate) cert;
              String subjectDn = x509Certificate.getSubjectDN().getName();
              try {
                x509Certificate.checkValidity();
              } catch (CertificateExpiredException e) {
                monitorReportResults.add(createResult(checkTitle, "The certificate (" + subjectDn + ") of the service service (" + service + ") has expired", MonitorReportResultStatus.FAILURE));
                continue;
              } catch (CertificateNotYetValidException e) {
                monitorReportResults.add(createResult(checkTitle, "The certificate (" + subjectDn + ") of the service (" + service + ") is not yet valid", MonitorReportResultStatus.FAILURE));
                continue;
              }

              Date expirationDate = x509Certificate.getNotAfter();
              long duration = Duration.between(Instant.now(), expirationDate.toInstant()).toDays();
              if (duration < this.certificateExpirationDelayBeforeFailure) {
                monitorReportResults.add(createResult(checkTitle, "The certificate (" + subjectDn + ") of the service (" + service + ") expires over " + duration + " days", MonitorReportResultStatus.FAILURE));
                continue;
              }
              /**
               * We report not the certificate chain success only the first one
               */
              if (monitorReportResults.isEmpty()) {
                monitorReportResults.add(createResult(checkTitle, "The certificate (" + subjectDn + ") of the service (" + service + ") expires only over " + duration + " days", MonitorReportResultStatus.SUCCESS));
              }
            }
            return Future.succeededFuture(monitorReportResults);
          },
          err -> {
            List<MonitorReportResult> monitorReportResults = new ArrayList<>();
            monitorReportResults.add(createResult(checkTitle, "Unable to connect to the service (" + port + "). Message:" + err.getMessage(), MonitorReportResultStatus.FAILURE));
            return Future.succeededFuture(monitorReportResults);
          }
        );
      this.addAsyncListResult(dnsName, futureResults);
    }

  }

  private void addAsyncListResult(DnsName dnsName, Future<List<MonitorReportResult>> futureResults) {
    this.asyncListResults.put(dnsName, futureResults);
  }

  public List<MonitorReport> getMonitorReports() {

    LOGGER.info("Monitor Creating report");
    HashSet<DnsName> allNames = new HashSet<>(reports.keySet());
    allNames.addAll(asyncResults.keySet());
    allNames.addAll(asyncListResults.keySet());
    List<MonitorReport> monitorReports = new ArrayList<>();
    for (DnsName dnsName : allNames) {

      MonitorReport monitorReport = new MonitorReport("Dns check for " + dnsName);
      monitorReports.add(monitorReport);
      // Sync
      List<MonitorReportResult> monitorReportResults = reports.get(dnsName);
      if (monitorReportResults != null) {
        for (MonitorReportResult monitorReportResult : monitorReportResults) {
          monitorReport.addResult(monitorReportResult);
        }
      }
      // Async
      List<Future<MonitorReportResult>> asyncMonitorReportResults = asyncResults.get(dnsName);
      if (asyncMonitorReportResults != null) {
        for (Future<MonitorReportResult> monitorReportResult : asyncMonitorReportResults) {
          monitorReport.addFutureResult(monitorReportResult);
        }
      }
      // Async List
      Future<List<MonitorReportResult>> asyncListMonitorReportResults = asyncListResults.get(dnsName);
      if (asyncListMonitorReportResults != null) {
        monitorReport.addFutureResults(asyncListMonitorReportResults);
      }
    }
    return monitorReports;
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
  private void checkDkim(Set<DnsName> domains) {

    String checkName = "Dkim check";
    for (DnsName domain : domains) {

      for (String selector : domain.getExpectedDkimSelector()) {
        String domainDkimTextRecord;
        try {
          domainDkimTextRecord = dnsClient.lookupDkimRecord(domain,selector);
        } catch (DnsException e) {
          this.addFailure(checkName, domain, "An exception has occurred: " + e.getMessage());
          continue;
        } catch (DnsNotFoundException e) {
          this.addFailure(checkName, domain, "The dkim (" + selector + ") was not found.");
          continue;
        }
        String expectedValue = domain.getExpectedDkimValue(selector);
        if (domainDkimTextRecord.equals(expectedValue)) {
          this.addSuccess(checkName, domain, "The dkim (" + selector + ") was found with the good value.");
        } else {
          this.addFailure(checkName, domain, "The dkim (" + selector + ") was found with a BAD value. " + MonitorReport.CRLF +
            MonitorReport.TAB2 + "Expected DKim is: " + expectedValue + MonitorReport.CRLF +
            MonitorReport.TAB2 + "Actual DKim is: " + domainDkimTextRecord
          );
        }
      }
    }
  }

  private MonitorServices checkDmarc(Set<DnsName> domains) {

    String checkName = "Dmarc check";
    /**
     * For external domain, the EDV records should contain
     * at least this value
     */
    String expectedEdvSuffixValue = "v=DMARC1";
    for (DnsName domain : domains) {
      /**
       * Dmarc
       */
      try {
        String dmarc = dnsClient.lookupDmarc(domain);
        String expectedDmarc = domain.getExpectedDmarcRecord();
        if (dmarc.equals(expectedDmarc)) {
          this.addSuccess(checkName, domain, "The dmarc is correct");
        } else {
          this.addFailure(checkName, domain, "The dmarc is incorrect. It should be (" + expectedDmarc + ") and not (" + dmarc + ")");
        }
      } catch (DnsException e) {
        this.addFailure(checkName, domain, "The dmarc record query fires an exception: " + e.getMessage());
      } catch (DnsNotFoundException e) {
        this.addFailure(checkName, domain, "The dmarc record was not found");
      }
      /**
       * EDV (external domain verification (EDV))
       * Non-first party dmarc email domain should add a record known as EDV in their
       * domain to allow it.
       * See https://datatracker.ietf.org/doc/html/rfc7489#section-7.1
       * <p>
       */
      for (EmailAddress email : domain.getDmarcEmails()) {
        DnsName emailDomain;
        try {
          emailDomain = email.getDomainName();
          if (!emailDomain.equals(domain)) {
            DnsName dmarcReportName = emailDomain
              .getSubdomain("_dmarc")
              .getSubdomain("_report");
            DnsName edvDomainName = dmarcReportName
              .getSubdomain(domain.toStringWithoutRoot());
            try {
              String edvValue = dnsClient.lookupTxt(edvDomainName);
              if (!edvValue.trim().startsWith(expectedEdvSuffixValue)) {
                this.addFailure(checkName, dmarcReportName, "The edv dmarc txt record should be (" + expectedEdvSuffixValue + "), not (" + edvValue + ")");
              } else {
                this.addSuccess(checkName, dmarcReportName, "The edv dmarc record value for the name (" + edvDomainName + ") is " + edvValue);
              }
            } catch (DnsException e) {
              this.addFailure(checkName, dmarcReportName, "The edv dmarc record query fires an exception: " + e.getMessage());
            } catch (DnsNotFoundException e) {
              /**
               * A Report Receiver that is willing to receive reports for any domain
               * can use a wildcard DNS record.
               * For example, a TXT resource record at "*._report._dmarc.example.com"
               */
              DnsName edvWildcardName = dmarcReportName.getSubdomain("*");
              try {
                String edvWildcardValue = dnsClient.lookupTxt(edvWildcardName);
                if (!edvWildcardValue.trim().startsWith(expectedEdvSuffixValue)) {
                  this.addFailure(checkName, dmarcReportName, "The edv wildcard dmarc txt record (" + edvWildcardName + ") should be (" + expectedEdvSuffixValue + "), not (" + edvWildcardValue + ")");
                } else {
                  this.addSuccess(checkName, dmarcReportName, "The edv dmarc record value for the name (" + edvWildcardName + ") is " + edvWildcardValue);
                }
              } catch (DnsException wildcardException) {
                this.addFailure(checkName, dmarcReportName, "The edv dmarc record query (" + edvWildcardName + ") fires an exception: " + wildcardException.getMessage());
              } catch (DnsNotFoundException wildcardException) {
                this.addFailure(checkName, dmarcReportName, "An EDV dmarc text record was not found under one of theses names (" + edvDomainName + "," + edvWildcardName + ")");
              }
            }
          }
        } catch (CastException e) {
          throw new RuntimeException("Illegal name should not happen", e);
        }
      }
    }

    return this;
  }

}
