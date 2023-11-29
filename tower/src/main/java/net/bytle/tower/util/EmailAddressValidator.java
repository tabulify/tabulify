package net.bytle.tower.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import jakarta.mail.internet.AddressException;
import net.bytle.dns.DnsBlockList;
import net.bytle.dns.DnsBlockListQuery;
import net.bytle.dns.DnsIllegalArgumentException;
import net.bytle.dns.DnsName;
import net.bytle.email.BMailInternetAddress;
import net.bytle.html.HtmlGrading;
import net.bytle.html.HtmlStructureException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.HttpClientBuilder;

import java.util.ArrayList;
import java.util.List;

public class EmailAddressValidator {


  private final WebClient webClient;
  private final TowerDnsClient dnsClient;

  public EmailAddressValidator(EraldyApiApp eraldyApiApp) {
    Vertx vertx = eraldyApiApp.getApexDomain().getHttpServer().getServer().getVertx();
    webClient = HttpClientBuilder.builder(vertx)
      .setMaxHeaderSize(8192 * 10) // mail.ru has HTTP headers that are bigger than 8192 bytes
      .setConnectTimeout(1000) // 1 second, 163.com
      .buildWebClient();
    dnsClient = new TowerDnsClient(eraldyApiApp);
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
    DnsName emailDomain;
    try {
      emailDomain = DnsName.create(mail.getDomain());
    } catch (DnsIllegalArgumentException e) {
      return Future.failedFuture(e);
    }

    String mxValidCheck = "mxRecord";
    Future<EmailAddressValidityReport> mxRecords = dnsClient.resolveMx(emailDomain)
      .compose(records -> {
        String noMxRecordMessage = "The domain (" + emailDomain + ") has no MX records";

        if (records.size() == 0) {
          emailValidityReport.addError(mxValidCheck, noMxRecordMessage);
        } else {
          emailValidityReport.addSuccess(mxValidCheck, "Mx records were found");
        }
        return Future.succeededFuture(emailValidityReport);

      }, err -> {

        emailValidityReport.addError(mxValidCheck, err.getMessage());
        return Future.succeededFuture(emailValidityReport);

      });


    /**
     * A records
     */
    String aValidCheck = "aRecord";
    DnsName apexDomain = emailDomain.getApexName();
    String noARecordMessage = "The apex domain (" + apexDomain + ") has no A records";
    Future<EmailAddressValidityReport> aRecordFuture = dnsClient.resolveA(apexDomain)
      .compose(
        aRecords -> {
          if (aRecords.size() == 0) {
            emailValidityReport.addError(aValidCheck, noARecordMessage);
          } else {
            emailValidityReport.addSuccess(aValidCheck, "A records were found");
          }
          return Future.succeededFuture(emailValidityReport);
        }
        , err -> {
          emailValidityReport.addError(aValidCheck, err.getMessage());
          return Future.succeededFuture(emailValidityReport);
        });


    /**
     * Domain blocked?
     * (Not yet async)
     */
    String blockListCheck = "blockList";
    String apexDomainNameAsString = apexDomain.toStringWithoutRoot();
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
    Future<EmailAddressValidityReport> homePageFuture = webClient.getAbs(absoluteURI)
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

    /**
     * Composite execution
     */
    List<Future<EmailAddressValidityReport>> futureReports = new ArrayList<>();
    futureReports.add(mxRecords);
    futureReports.add(aRecordFuture);
    futureReports.add(homePageFuture);
    CompositeFuture compositeFutureReport;
    if(failEarly){
      compositeFutureReport = Future.all(futureReports);
    } else {
      compositeFutureReport = Future.join(futureReports);
    }
    return compositeFutureReport.compose(
      c->Future.succeededFuture(emailValidityReport),
      err->Future.succeededFuture(emailValidityReport)
    );

  }
}
