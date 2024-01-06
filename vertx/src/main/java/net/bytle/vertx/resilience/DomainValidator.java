package net.bytle.vertx.resilience;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import net.bytle.dns.*;
import net.bytle.html.HtmlGrading;
import net.bytle.html.HtmlStructureException;
import net.bytle.vertx.HttpClientBuilder;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerDnsClient;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DomainValidator {

  private final WebClient webClient;
  private final TowerDnsClient dnsClient;
  /**
   * ESP Domains
   */
  private final Set<String> whiteListDomains;


  public DomainValidator(TowerApp towerApp) {
    Server server = towerApp.getApexDomain().getHttpServer().getServer();
    webClient = HttpClientBuilder.builder(server.getVertx())
      .setMaxHeaderSize(8192 * 10) // mail.ru has HTTP headers that are bigger than 8192 bytes
      .setConnectTimeout(1000) // 1 second, 163.com
      .buildWebClient();
    dnsClient = server.getDnsClient();
    /**
     * The known white list that we don't test
     */
    whiteListDomains = new HashSet<>();
    whiteListDomains.add("gmail.com");
    whiteListDomains.add("googlemail.com");
    whiteListDomains.add("yahoo.com");
    whiteListDomains.add("outlook.com");
    whiteListDomains.add("hotmail.com");
    whiteListDomains.add("live.com");
    whiteListDomains.add("icloud.com");
    whiteListDomains.add("aol.com");
    whiteListDomains.add("protonmail.com");
    whiteListDomains.add("gmx.com");
    whiteListDomains.add("yandex.com");
    whiteListDomains.add("yandex.ru");
    whiteListDomains.add("mail.com");
    whiteListDomains.add("zoho.com");
  }

  /**
   *
   * @param dnsName - the domain to validate
   * @param failEarly - if true, will stop to the first failure and return it
   * @return a list of validation
   */
  public Future<DomainValidatorResult> validate(DnsName dnsName, boolean failEarly) {

    DomainValidatorResult domainValidatorResult = new DomainValidatorResult();

    /**
     * White List / Legit Domains
     */
    String lowerCaseDomainWithoutRoot = dnsName.toStringWithoutRoot().toLowerCase();
    if (whiteListDomains.contains(lowerCaseDomainWithoutRoot)) {
      domainValidatorResult.addTest(ValidationTest.WHITE_LIST.createResultBuilder()
        .setMessage("Domain (" + lowerCaseDomainWithoutRoot + ") is on the white list")
        .succeed());
      return Future.succeededFuture(domainValidatorResult);
    }

    /**
     * Mx record
     */
    ValidationTestResult.Builder mxValidCheck = ValidationTest.MX_RECORD.createResultBuilder();
    Future<ValidationTestResult.Builder> mxRecords = dnsClient.resolveMx(dnsName)
      .compose(records -> {
        String noMxRecordMessage = "The domain (" + dnsName + ") has no MX records";
        if (records.isEmpty()) {
          return Future.failedFuture(mxValidCheck.setMessage(noMxRecordMessage));
        } else {
          return Future.succeededFuture(mxValidCheck.setMessage("Mx records were found"));
        }
      }, err -> Future.failedFuture(mxValidCheck.setFatalError(err)));
    List<Future<ValidationTestResult.Builder>> compositeFutureList = new ArrayList<>();
    compositeFutureList.add(mxRecords);

    /**
     * A records
     */
    ValidationTestResult.Builder aValidCheck = ValidationTest.A_RECORD.createResultBuilder();
    DnsName apexDomain = dnsName.getApexName();
    Future<ValidationTestResult.Builder> aRecordFuture = dnsClient.resolveA(apexDomain)
      .compose(
        aRecords -> {

          if (aRecords.isEmpty()) {
            return Future.failedFuture(aValidCheck.setMessage("The apex domain (" + apexDomain + ") has no A records"));
          }
          return Future.succeededFuture(aValidCheck.setMessage("A records were found"));

        }
        , err -> Future.failedFuture(aValidCheck.setFatalError(err))
      );
    compositeFutureList.add(aRecordFuture);

    /**
     * Domain blocked?
     * (Not yet async)
     */
    ValidationTestResult.Builder blockListCheck = ValidationTest.BLOCK_LIST.createResultBuilder();
    String apexDomainNameAsString = apexDomain.toStringWithoutRoot();
    List<DnsBlockListQueryHelper> dnsBlockLists = DnsBlockListQueryHelper.forDomain(apexDomainNameAsString).addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG)
      .build();
    for (DnsBlockListQueryHelper dnsBlockListQueryHelper : dnsBlockLists) {
      DnsName dnsNameToQuery = dnsBlockListQueryHelper.getDnsNameToQuery();
      Future<ValidationTestResult.Builder> futureBlockListCheck = dnsClient.resolveA(dnsNameToQuery)
        .compose(dnsIps -> {

          if (dnsIps.isEmpty()) {
            return Future.succeededFuture(blockListCheck.setMessage("The apex domain (" + apexDomainNameAsString + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList()));
          }

          DnsIp dnsIp = dnsIps.iterator().next();
          DnsBlockListResponseCode dnsBlockListResponse = dnsBlockListQueryHelper.createResponseCode(dnsIp);
          if (dnsBlockListResponse.getBlocked()) {
            return Future.failedFuture(blockListCheck.setMessage("The apex domain (" + apexDomainNameAsString + ") is blocked by " + dnsBlockListQueryHelper.getBlockList()));
          }

          return Future.succeededFuture(blockListCheck.setMessage("The apex domain (" + apexDomainNameAsString + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList()));
        }, err -> Future.failedFuture(blockListCheck.setFatalError(err)));
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
    ValidationTestResult.Builder webServer = ValidationTest.WEB_SERVER.createResultBuilder();
    ValidationTestResult.Builder homePage = ValidationTest.HOME_PAGE.createResultBuilder();
    String absoluteURI = "https://" + apexDomainNameAsString;
    Future<ValidationTestResult.Builder> homePageFuture = webClient.getAbs(absoluteURI)
      .send()
      .compose(response -> {
          try {
            HtmlGrading.grade(response.bodyAsString());
            return Future.succeededFuture(homePage.setMessage("HTML page legit at (" + absoluteURI + ")"));
          } catch (HtmlStructureException e) {
            return Future.failedFuture(homePage.setMessage("The HTML page (" + absoluteURI + ") is not legit" + e.getMessage()));
          }
        },
        err -> {
          String message = "The web server (" + absoluteURI + ") could not be contacted.";
          Set<Class<?>> nonFatalError = new HashSet<>();
          // bad ssl (don't repeat)
          nonFatalError.add(javax.net.ssl.SSLHandshakeException.class);
          // unknown host (don't repeat)
          nonFatalError.add(UnknownHostException.class);
          // connection was closed
          nonFatalError.add(io.vertx.core.http.HttpClosedException.class);
          // time out
          nonFatalError.add(io.netty.channel.ConnectTimeoutException.class);
          if (nonFatalError.contains(err.getClass())) {
            return Future.failedFuture(webServer.setNonFatalError(err, message));
          }
          return Future.failedFuture(webServer.setFatalError(err, message));
        }
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
        results -> Future.succeededFuture(
          domainValidatorResult.addTests(results
            .list()
            .stream()
            .map(e -> ((ValidationTestResult.Builder) e).succeed())
            .collect(Collectors.toSet())
          )
        ),
        err -> {
          if (!(err instanceof ValidationTestResult.Builder)) {
            return Future.failedFuture(err);
          }
          Set<ValidationTestResult> validationTestResults = new HashSet<>();
          // We add the error to be sure that we register the error
          // The set make it certain that we don't get 2 validation results
          ValidationTestResult validationError = ((ValidationTestResult.Builder) err).fail();
          validationTestResults.add(validationError);
          for (int i = 0; i < compositeFuture.size(); i++) {
            if (compositeFuture.failed(i)) {
              Throwable cause = compositeFuture.cause(i);
              if (cause instanceof ValidationTestResult.Builder) {
                ValidationTestResult validationTestResultBuilder = ((ValidationTestResult.Builder) cause).fail();
                validationTestResults.add(validationTestResultBuilder);
                continue;
              }
              return Future.failedFuture(cause);
            }
            if (compositeFuture.succeeded(i)) {
              ValidationTestResult validationTestResult = ((ValidationTestResult.Builder) compositeFuture.resultAt(i)).succeed();
              validationTestResults.add(validationTestResult);
            }
          }
          return Future.succeededFuture(domainValidatorResult.addTests(validationTestResults));
        }
      );
  }
}
