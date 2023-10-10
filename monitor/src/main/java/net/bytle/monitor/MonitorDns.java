package net.bytle.monitor;

import net.bytle.dns.*;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * To finish it, see
 * <a href="https://support.google.com/mail/answer/81126">...</a>
 */
public class MonitorDns {

  public static final String DATACADAMIA_COM = "datacadamia.com";
  private final String COMBOSTRAP_DOT_COM = "combostrap.com";

  private final List<String> DOMAINS = Arrays.asList(
    "bytle.net",
    COMBOSTRAP_DOT_COM,
    DATACADAMIA_COM,
    "eraldy.com",
    "gerardnico.com",
    "persso.com",
    "tabulify.com"
  );


  public static final String BEAU_SERVER_NAME = "beau.bytle.net";

  public static final String BEAU_SERVER_IPV4 = "192.99.55.226";

  public static final String BEAU_SERVER_IPV6 = "2607:5300:201:3100::85b";

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

  public void checkPtrTest() throws UnknownHostException, ExecutionException, InterruptedException {

    // Get the IP address associated with a name
    InetAddress ipAddress = DnsName.createFrom(BEAU_SERVER_NAME).getIpAddress();
//    Assert.assertEquals(BEAU_SERVER_IPV4, ipAddress.getHostAddress());

    // Get the name associated with the IP
    PTRRecord ptrNameV6 = DnsAddress.createFromIpv6String(BEAU_SERVER_IPV6).getPtrRecordAsync();
    Name target = ptrNameV6.getTarget();
//    Assert.assertEquals(target.toString(), BEAU_SERVER_NAME + DnsUtil.ABSOLUTE_TRAILING_DOT);

    PTRRecord ptrNameV4 = DnsAddress.createFromIpv4String(BEAU_SERVER_IPV4).getPtrRecordAsync();
    Name targetV4 = ptrNameV4.getTarget();
//    Assert.assertEquals(targetV4.toString(), BEAU_SERVER_NAME + DnsUtil.ABSOLUTE_TRAILING_DOT);

  }


  public void checkSpfTest() throws IOException {

    /**
     * The spf record that should be included in all domains
     */
    String expectedComboSpfTxtRecord = "v=spf1 mx ip4:" + BEAU_SERVER_IPV4 + "/32 ip6:" + BEAU_SERVER_IPV6 + " include:spf.mailjet.com include:_spf.google.com include:spf.mandrillapp.com ~all";
    DnsDomain combostrapDomain = DnsDomain.createFrom(COMBOSTRAP_DOT_COM);
    TXTRecord spfRecord = combostrapDomain
      .getSpfRecord();

//    Assert.assertEquals("spf is good", expectedComboSpfTxtRecord, DnsUtil.getStringFromTxtRecord(spfRecord));
    String comboSpfTextRecordName = combostrapDomain.getSpfARecordName();
    /**
     * The spf record for all domains
     */
    String expectedSpfRecord = "v=spf1 include:" + comboSpfTextRecordName + " -all";
    List<TXTRecord> foundSPFRecordsByDomain = new ArrayList<>();
    for (String domain : DOMAINS) {
      DnsDomain dnsDomain = DnsDomain.createFrom(domain);
      TXTRecord domainSpfTextRecord = dnsDomain.getSpfRecord();
//      Assert.assertNotNull("The spf record for the name (" + dnsDomain.getSpfARecordName() + ") is not null", domainSpfTextRecord);
      String actualSpfStringValue = DnsUtil.getStringFromTxtRecord(domainSpfTextRecord);
      if (actualSpfStringValue.startsWith(DnsDomain.getSpfPrefix())) {
//        Assert.assertEquals("The spf record for the name (" + dnsDomain.getSpfARecordName() + ") should be good", expectedSpfRecord, actualSpfStringValue);
        foundSPFRecordsByDomain.add(domainSpfTextRecord);
      }
    }
//    Assert.assertEquals("All spf records where found", foundSPFRecordsByDomain.size(), DOMAINS.size());

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

    for (String domain : DOMAINS) {
      DnsDomain dnsDomain = DnsDomain.createFrom(domain);
      TXTRecord domainDkimTextRecord = dnsDomain.getDkimRecord(dkimSelector);
      //Assert.assertNotNull("The dkim record for the name (" + dnsDomain.getDkimName(dkimSelector) + ") was not found", domainDkimTextRecord);
      if (checkValue) {
        String actualDkimStringValue = DnsUtil.getStringFromTxtRecord(domainDkimTextRecord);
        //Assert.assertEquals("The dkim record for the name (" + dnsDomain.getDkimName(dkimSelector) + ") should be good", expectedDkimValue, actualDkimStringValue);
      }
    }

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
