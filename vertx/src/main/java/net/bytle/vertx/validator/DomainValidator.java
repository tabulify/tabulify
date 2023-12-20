package net.bytle.vertx.validator;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import net.bytle.dns.*;
import net.bytle.html.HtmlGrading;
import net.bytle.html.HtmlStructureException;
import net.bytle.vertx.*;

import java.util.ArrayList;
import java.util.List;

public class DomainValidator {

  private final WebClient webClient;
  private final TowerDnsClient dnsClient;


  public DomainValidator(TowerApp towerApp) {
    Server server = towerApp.getApexDomain().getHttpServer().getServer();
    webClient = HttpClientBuilder.builder(server.getVertx())
      .setMaxHeaderSize(8192 * 10) // mail.ru has HTTP headers that are bigger than 8192 bytes
      .setConnectTimeout(1000) // 1 second, 163.com
      .buildWebClient();
    dnsClient = server.getDnsClient();
  }

  /**
   *
   * @param dnsName - the domain to validate
   * @param failEarly - if true, will stop to the first failure and return it
   * @return a list of validation
   */
  public Future<List<ValidationResult>> validate(DnsName dnsName, boolean failEarly) {

    /**
     * Mx record
     */
    ValidationResult.Builder mxValidCheck = ValidationResult.builder("mxRecord");
    Future<ValidationResult> mxRecords = dnsClient.resolveMx(dnsName)
      .compose(records -> {
        String noMxRecordMessage = "The domain (" + dnsName + ") has no MX records";
        if (records.isEmpty()) {
          return Future.failedFuture(mxValidCheck.fail(noMxRecordMessage));
        } else {
          return Future.succeededFuture(mxValidCheck.succeed("Mx records were found"));
        }
      }, err -> Future.failedFuture(mxValidCheck.fail(err)));
    List<Future<ValidationResult>> compositeFutureList = new ArrayList<>();
    compositeFutureList.add(mxRecords);

    /**
     * A records
     */
    ValidationResult.Builder aValidCheck = ValidationResult.builder("aRecord");
    DnsName apexDomain = dnsName.getApexName();
    Future<ValidationResult> aRecordFuture = dnsClient.resolveA(apexDomain)
      .compose(
        aRecords -> {

          if (aRecords.isEmpty()) {
            return Future.failedFuture(aValidCheck.fail("The apex domain (" + apexDomain + ") has no A records"));
          }
          return Future.succeededFuture(aValidCheck.succeed("A records were found"));

        }
        , err -> Future.failedFuture(aValidCheck.fail(err))
      );
    compositeFutureList.add(aRecordFuture);

    /**
     * Domain blocked?
     * (Not yet async)
     */
    ValidationResult.Builder blockListCheck = ValidationResult.builder("blockList");
    String apexDomainNameAsString = apexDomain.toStringWithoutRoot();
    List<DnsBlockListQueryHelper> dnsBlockLists = DnsBlockListQueryHelper.forDomain(apexDomainNameAsString).addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG)
      .build();
    for (DnsBlockListQueryHelper dnsBlockListQueryHelper : dnsBlockLists) {
      DnsName dnsNameToQuery = dnsBlockListQueryHelper.getDnsNameToQuery();
      Future<ValidationResult> futureBlockListCheck = dnsClient.resolveA(dnsNameToQuery)
        .compose(dnsIps -> {

          if (dnsIps.isEmpty()) {
            return Future.failedFuture(blockListCheck.succeed("The apex domain (" + apexDomainNameAsString + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList()));
          }

          DnsIp dnsIp = dnsIps.iterator().next();
          DnsBlockListResponseCode dnsBlockListResponse = dnsBlockListQueryHelper.createResponseCode(dnsIp);
          if (dnsBlockListResponse.getBlocked()) {
            return Future.failedFuture(blockListCheck.fail("The apex domain (" + apexDomainNameAsString + ") is blocked by " + dnsBlockListQueryHelper.getBlockList()));
          }

          return Future.succeededFuture(blockListCheck.succeed("The apex domain (" + apexDomainNameAsString + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList()));
        }, err -> Future.failedFuture(blockListCheck.fail(err)));
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
    ValidationResult.Builder webServer = ValidationResult.builder("webServer");
    ValidationResult.Builder homePage = ValidationResult.builder("homePage");
    String absoluteURI = "https://" + apexDomainNameAsString;
    Future<ValidationResult> homePageFuture = webClient.getAbs(absoluteURI)
      .send()
      .compose(response -> {
          try {
            HtmlGrading.grade(response.bodyAsString());
            return Future.succeededFuture(homePage.succeed("HTML page legit at (" + absoluteURI + ")"));
          } catch (HtmlStructureException e) {
            return Future.failedFuture(homePage.fail("The HTML page (" + absoluteURI + ") is not legit" + e.getMessage()));
          }
        },
        err -> Future.failedFuture(webServer.fail(err, "The web server (" + absoluteURI + ") could not be contacted."))
      );
    compositeFutureList.add(homePageFuture);


    /**
     * Composite execution
     */
    CompositeFuture compositeFuture;
    if (failEarly) {
      compositeFuture = Future.all(compositeFutureList);
    } else {
      compositeFuture = Future.join(compositeFutureList);
    }
    return compositeFuture
      .compose(
        results -> Future.succeededFuture(results.list()),
        err -> {
          if (!(err instanceof ValidationResult)) {
            return Future.failedFuture(err);
          }
          List<ValidationResult> validationResults = new ArrayList<>();
          validationResults.add((ValidationResult) err);
          for (Object validationResult : compositeFuture.list()) {
            // not completed future are null
            if (validationResult != null) {
              validationResults.add((ValidationResult) validationResult);
            }
          }
          return Future.succeededFuture(validationResults);
        }
      );
  }
}
