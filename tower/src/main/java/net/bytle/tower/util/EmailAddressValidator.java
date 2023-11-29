package net.bytle.tower.util;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import jakarta.mail.internet.AddressException;
import net.bytle.dns.*;
import net.bytle.email.BMailInternetAddress;
import net.bytle.html.HtmlGrading;
import net.bytle.html.HtmlStructureException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.HttpClientBuilder;

import java.util.List;
import java.util.Set;

public class EmailAddressValidator {


  private final WebClient webClient;

  public EmailAddressValidator(EraldyApiApp eraldyApiApp) {
    Vertx vertx = eraldyApiApp.getApexDomain().getHttpServer().getServer().getVertx();
    webClient = HttpClientBuilder.builder(vertx)
      .setMaxHeaderSize(8192*10) // mail.ru has HTTP headers that are bigger than 8192 bytes
      .setConnectTimeout(1000) // 1 second, 163.com
      .buildWebClient();
  }


  /**
   * @param email - the email to valid
   * @return {@link EmailAddressValidityReport}
   */
  public Future<EmailAddressValidityReport> validate(String email, boolean failEarly) {
    BMailInternetAddress mail;

    String emailValidCheck = "emailAddress";
    EmailAddressValidityReport emailValidityReport = new EmailAddressValidityReport(email);
    try {
      mail = BMailInternetAddress.of(email);
    } catch (AddressException e) {
      emailValidityReport.addError(emailValidCheck, "Email address not valid");
      return Future.succeededFuture(emailValidityReport);
    }
    emailValidityReport.addSuccess(emailValidCheck, "Email address is valid");


    /**
     * Mx text
     */
    String domain = mail.getDomain();
    String mxValidCheck = "mxRecord";
    XBillDnsClient dnsClient = XBillDnsClient.builder().build();

    DnsName name;
    try {
      name = dnsClient.createDnsName(domain);
    } catch (DnsIllegalArgumentException e) {
      return Future.failedFuture(new RuntimeException("Internal error: The domain (" + domain + ") is not valid. The email is valid, The domain should be valid", e));
    }
    String noMxRecordMessage = "The domain (" + name + ") has no MX records";
    try {
      List<DnsMxRecord> records = name.getMxRecords();
      if (records.size() == 0) {
        emailValidityReport.addError(mxValidCheck, noMxRecordMessage);
      } else {
        emailValidityReport.addSuccess(mxValidCheck, "Mx records were found");
      }
    } catch (DnsNotFoundException e) {
      emailValidityReport.addError(mxValidCheck, noMxRecordMessage);
      if (failEarly) {
        return Future.succeededFuture(emailValidityReport);
      }
    } catch (DnsException e) {
      return Future.failedFuture(e);
    }


    /**
     * A records
     */
    String aValidCheck = "aRecord";
    DnsName apexName = name.getApexName();
    String noARecordMessage = "The apex domain (" + apexName + ") has no A records";
    try {
      Set<DnsIp> aRecords = apexName.resolveA();
      if (aRecords.size() == 0) {
        emailValidityReport.addError(aValidCheck, noARecordMessage);
      } else {
        emailValidityReport.addSuccess(aValidCheck, "A records were found");
      }
    } catch (DnsNotFoundException e) {
      emailValidityReport.addError(aValidCheck, noARecordMessage);
      if (failEarly) {
        return Future.succeededFuture(emailValidityReport);
      }
    } catch (DnsException e) {
      return Future.failedFuture(e);
    }


    /**
     * Domain blocked?
     */
    String blockListCheck = "blockList";
    String apexDomainNameAsString = apexName.toStringWithoutRoot();
    boolean blocked = DnsBlockListQuery.forDomain(apexDomainNameAsString)
      .addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG)
      .query()
      .getFirst()
      .getBlocked();
    if (blocked) {
      emailValidityReport.addError(blockListCheck, "The apex domain (" + apexDomainNameAsString + ") is blocked by SpamHaus");
      if (failEarly) {
        return Future.succeededFuture(emailValidityReport);
      }
    } else {
      emailValidityReport.addSuccess(blockListCheck, "The apex domain (" + apexDomainNameAsString + ") is not blocked by SpamHaus");
    }

    /**
     * HTML page test
     * (Certificate)
     */
    // https with valid certificate at minima
    // content: one image at minima
    // example:
    // http://take-ur-vites.org/

    // 163.com (tou522884141@163.com, ...)
    // http://poker40.com/
    // isaymur7rw5@bigmir.net
    // gsalike@mail.ru
    String homePage = "homePage";
    String absoluteURI = "https://" + apexDomainNameAsString;
    return webClient.getAbs(absoluteURI)
      .send()
      .compose(response -> {
          try {
            HtmlGrading.grade(response.bodyAsString());
            emailValidityReport.addSuccess(homePage, "HTML page legit at (" + absoluteURI + ")");
          } catch (HtmlStructureException e) {
            emailValidityReport.addError(homePage, "The HTML page (" + absoluteURI + ") is not legit" + e.getMessage());
          }
          return Future.succeededFuture(emailValidityReport);
        },
        err -> Future.succeededFuture(emailValidityReport.addError(homePage, "The HTTP website (" + absoluteURI + ") could not be contacted. Error: " + err.getClass().getSimpleName() + ": " + err.getMessage()))
      );
  }
}
