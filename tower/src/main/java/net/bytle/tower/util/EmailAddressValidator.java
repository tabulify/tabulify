package net.bytle.tower.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import jakarta.mail.internet.AddressException;
import net.bytle.dns.*;
import net.bytle.email.BMailInternetAddress;
import net.bytle.html.HtmlGrading;
import net.bytle.html.HtmlStructureException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.HttpClientBuilder;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerDnsClient;

import java.util.ArrayList;
import java.util.List;

public class EmailAddressValidator {


  private final WebClient webClient;
  private final TowerDnsClient dnsClient;

  public EmailAddressValidator(EraldyApiApp eraldyApiApp) {
    Server server = eraldyApiApp.getApexDomain().getHttpServer().getServer();
    webClient = HttpClientBuilder.builder(server.getVertx())
      .setMaxHeaderSize(8192 * 10) // mail.ru has HTTP headers that are bigger than 8192 bytes
      .setConnectTimeout(1000) // 1 second, 163.com
      .buildWebClient();
    dnsClient = server.getDnsClient();
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
     * The domain to check
     */
    DnsName emailDomain;
    try {
      emailDomain = DnsName.create(mail.getDomain());
    } catch (DnsIllegalArgumentException e) {
      return Future.failedFuture(e);
    }

    /**
     * Composite execution for all checks
     */
    List<Future<Void>> compositeFutureList = new ArrayList<>();

    /**
     * Mx record
     */
    String mxValidCheck = "mxRecord";
    Future<Void> mxRecords = dnsClient.resolveMx(emailDomain)
      .compose(records -> {
        String noMxRecordMessage = "The domain (" + emailDomain + ") has no MX records";

        if (records.size() == 0) {
          emailValidityReport.addError(mxValidCheck, noMxRecordMessage);
        } else {
          emailValidityReport.addSuccess(mxValidCheck, "Mx records were found");
        }
        return Future.succeededFuture();

      }, err -> {

        emailValidityReport.addError(mxValidCheck, err.getMessage());
        return Future.succeededFuture();

      });
    compositeFutureList.add(mxRecords);

    /**
     * A records
     */
    String aValidCheck = "aRecord";
    DnsName apexDomain = emailDomain.getApexName();
    String noARecordMessage = "The apex domain (" + apexDomain + ") has no A records";
    Future<Void> aRecordFuture = dnsClient.resolveA(apexDomain)
      .compose(
        aRecords -> {
          if (aRecords.size() == 0) {
            emailValidityReport.addError(aValidCheck, noARecordMessage);
          } else {
            emailValidityReport.addSuccess(aValidCheck, "A records were found");
          }
          return Future.succeededFuture();
        }
        , err -> {
          emailValidityReport.addError(aValidCheck, err.getMessage());
          return Future.succeededFuture();
        });
    compositeFutureList.add(aRecordFuture);

    /**
     * Domain blocked?
     * (Not yet async)
     */
    String blockListCheck = "blockList";
    String apexDomainNameAsString = apexDomain.toStringWithoutRoot();
    List<DnsBlockListQueryHelper> dnsBlockLists = DnsBlockListQueryHelper.forDomain(apexDomainNameAsString).addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG)
      .build();
    for(DnsBlockListQueryHelper dnsBlockListQueryHelper : dnsBlockLists){
      DnsName dnsNameToQuery = dnsBlockListQueryHelper.getDnsNameToQuery();
      Future<Void> futureBlockListCheck =  dnsClient.resolveA(dnsNameToQuery)
        .compose(dnsIps->{
          if(dnsIps.size()==0){
             emailValidityReport.addSuccess(blockListCheck, "The apex domain (" + apexDomainNameAsString + ") is not blocked by "+ dnsBlockListQueryHelper.getBlockList());
            return Future.succeededFuture();
          }
          DnsIp dnsIp = dnsIps.iterator().next();
          DnsBlockListResponseCode dnsBlockListResponse = dnsBlockListQueryHelper.createResponseCode(dnsIp);
          if(dnsBlockListResponse.getBlocked()){
            emailValidityReport.addError(blockListCheck, "The apex domain (" + apexDomainNameAsString + ") is blocked by "+ dnsBlockListQueryHelper.getBlockList());
          } else {
            emailValidityReport.addSuccess(blockListCheck, "The apex domain (" + apexDomainNameAsString + ") is not blocked by "+ dnsBlockListQueryHelper.getBlockList());
          }
          return Future.succeededFuture();
        }, err->{
          emailValidityReport.addError(blockListCheck, "Block list query error: "+err.getMessage());
          return Future.succeededFuture();
        });
      compositeFutureList.add(futureBlockListCheck);
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
    Future<Void> homePageFuture = webClient.getAbs(absoluteURI)
      .send()
      .compose(response -> {
          try {
            HtmlGrading.grade(response.bodyAsString());
            emailValidityReport.addSuccess(homePage, "HTML page legit at (" + absoluteURI + ")");
          } catch (HtmlStructureException e) {
            emailValidityReport.addError(homePage, "The HTML page (" + absoluteURI + ") is not legit" + e.getMessage());
          }
          return Future.succeededFuture();
        },
        err -> {
          emailValidityReport.addError(homePage, "The HTTP website (" + absoluteURI + ") could not be contacted. Error: " + err.getClass().getSimpleName() + ": " + err.getMessage());
          return Future.succeededFuture();
        }
      );
    compositeFutureList.add(homePageFuture);


    /**
     * Composite execution
     */
    CompositeFuture compositeFutureReport;
    if (failEarly) {
      compositeFutureReport = Future.all(compositeFutureList);
    } else {
      compositeFutureReport = Future.join(compositeFutureList);
    }
    return compositeFutureReport.compose(
      c -> Future.succeededFuture(emailValidityReport),
      err -> Future.succeededFuture(emailValidityReport)
    );

  }
}
