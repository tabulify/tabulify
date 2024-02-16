package net.bytle.vertx.resilience;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.mail.MessagingException;
import net.bytle.dns.*;
import net.bytle.email.BMailSmtpClient;
import net.bytle.email.BMailStartTls;
import net.bytle.exception.AssertionException;
import net.bytle.exception.NotFoundException;
import net.bytle.html.HtmlGrading;
import net.bytle.html.HtmlStructureException;
import net.bytle.type.DnsName;
import net.bytle.vertx.HttpClientBuilder;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerDnsClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLHandshakeException;
import java.util.*;
import java.util.stream.Collectors;

public class DomainValidator {

  private static final Logger LOGGER = LogManager.getLogger(DomainValidator.class);
  private static final String VALIDATOR_DOMAIN_CHECK_PING_MX_CONF = "validator.domain.check.ping.mx";
  private final WebClient webClient;
  private final TowerDnsClient dnsClient;
  /**
   * ESP Domains
   */
  private final Set<String> whiteListDomains;
  private final Vertx vertx;
  private final Boolean checkPingMx;

  /**
   * Grey List are not allowed on import
   * but via email validation
   */
  private final HashSet<String> greyListDomains;
  private final HashSet<String> blockListDomains;


  public DomainValidator(TowerApp towerApp) {
    Server server = towerApp.getApexDomain().getHttpServer().getServer();
    webClient = HttpClientBuilder.builder(server.getVertx())
      .setMaxHeaderSize(8192 * 10) // mail.ru has HTTP headers that are bigger than 8192 bytes
      .setConnectTimeout(1000) // 1 second, 163.com
      .buildWebClient();
    dnsClient = server.getDnsClient();
    vertx = server.getVertx();
    // At home, the port 25 may be firewalld by the USP
    this.checkPingMx = server.getConfigAccessor().getBoolean(VALIDATOR_DOMAIN_CHECK_PING_MX_CONF, true);
    LOGGER.info("Ping check Mx ( " + VALIDATOR_DOMAIN_CHECK_PING_MX_CONF + ") was set to " + this.checkPingMx + ". The validation will try to connect to the SMTP server");

    /**
     * The known white list that we don't test
     */
    whiteListDomains = new HashSet<>();
    whiteListDomains.add("aol.com");
    whiteListDomains.add("bigmir.net"); // ukrainian portal
    whiteListDomains.add("free.fr");
    whiteListDomains.add("hotmail.com");
    whiteListDomains.add("icloud.com");
    whiteListDomains.add("gmail.com");
    whiteListDomains.add("gmx.com");
    whiteListDomains.add("googlemail.com");
    whiteListDomains.add("outlook.com");
    whiteListDomains.add("live.com");
    whiteListDomains.add("mail.com");
    whiteListDomains.add("mail.ru"); // mail.ru: https://en.wikipedia.org/wiki/VK_(company)
    whiteListDomains.add("orange.fr"); // mail.ru: https://en.wikipedia.org/wiki/VK_(company)
    whiteListDomains.add("protonmail.com");
    whiteListDomains.add("wanadoo.fr");
    whiteListDomains.add("yahoo.com");
    whiteListDomains.add("yandex.com");
    whiteListDomains.add("yandex.ru");
    whiteListDomains.add("zoho.com");

    /**
     * The known blocked domain list that we don't test
     */
    greyListDomains = new HashSet<>();
    greyListDomains.add("163.com");

    /**
     * Blocking
     */
    blockListDomains = new HashSet<>();
    blockListDomains.add("backlinksgenerator.in");
    blockListDomains.add("horsetipstersreview.com"); // betting platform
    blockListDomains.add("tremunpiercing.com"); // 3 email prueba3@..., prueba4@..., prueba5@...


  }

  /**
   * @param dnsName   - the domain to validate
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

    if (greyListDomains.contains(lowerCaseDomainWithoutRoot)) {
      domainValidatorResult.addTest(
        ValidationTest.GREY_LIST.createResultBuilder()
          .setMessage("Domain (" + lowerCaseDomainWithoutRoot + ") is on the grey list")
          .fail()
      );
      return Future.succeededFuture(domainValidatorResult);
    }
    if (blockListDomains.contains(lowerCaseDomainWithoutRoot)) {
      domainValidatorResult.addTest(
        ValidationTest.BLOCK_LIST.createResultBuilder()
          .setMessage("Domain (" + lowerCaseDomainWithoutRoot + ") is on the block list")
          .fail()
      );
      return Future.succeededFuture(domainValidatorResult);
    }

    /**
     * No email domain where we need to send an email has more than 2 labels
     * (example: `mail.backlinksgenerator.in`)
     */
    if(!dnsName.isApexDomain()){
      domainValidatorResult.addTest(
        ValidationTest.APEX_DOMAIN.createResultBuilder()
          .setMessage("Domain (" + lowerCaseDomainWithoutRoot + ") is not an apex domain")
          .fail()
      );
      if(failEarly){
        return Future.succeededFuture(domainValidatorResult);
      }
    } else {
      domainValidatorResult.addTest(
        ValidationTest.APEX_DOMAIN.createResultBuilder()
          .setMessage("Domain (" + lowerCaseDomainWithoutRoot + ") is an apex domain")
          .succeed()
      );
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
          List<DnsMxRecord> mxDnsRecords = records
            .stream()
            .sorted(Comparator.comparingInt(DnsMxRecord::getPriority))
            .collect(Collectors.toList());
          String mxLists = mxDnsRecords.stream().map(r -> r.getTarget().toStringWithoutRoot()).collect(Collectors.joining(", "));
          return vertx.executeBlocking(() -> {

              if (!this.checkPingMx) {
                return null;
              }

              Throwable smtpException = null;
              String smtpHost;
              for (DnsMxRecord dnsMxRecord : mxDnsRecords) {

                smtpHost = dnsMxRecord.getTarget().toStringWithoutRoot();
                String finalSmtpHost = smtpHost;
                try {
                  BMailSmtpClient.create()
                    .setPort(25)
                    .setHost(finalSmtpHost)
                    .setStartTls(BMailStartTls.ENABLE)
                    .setConnectionTimeout(1000)
                    .build()
                    .ping();
                  return null;
                } catch (MessagingException e) {
                  smtpException = e;
                }
              }
              return smtpException;
            })
            .compose(smtpError -> {
              String message = "Mx records were found";
              if (smtpError == null) {
                if (this.checkPingMx) {
                  message += " and smtp servers pinged (" + mxLists + ")";
                } else {
                  message += " and smtp servers ping is disabled (" + mxLists + ")";
                }
                return Future.succeededFuture(mxValidCheck.setMessage(message));
              } else {
                return Future.failedFuture(mxValidCheck.setFatalError(smtpError, message + " but no smtp server has responded (" + mxLists + ")"));
              }
            });
        }
      }, err -> Future.failedFuture(mxValidCheck.setFatalError(err)));

    /**
     * The DNS checks
     */
    List<Future<ValidationTestResult.Builder>> dnsChecksFutureTests = new ArrayList<>();
    dnsChecksFutureTests.add(mxRecords);

    /**
     * A records
     */
    ValidationTestResult.Builder aRecordsValidCheck = ValidationTest.A_RECORD.createResultBuilder();
    DnsName apexDomain = dnsName.getApexName();
    Future<ValidationTestResult.Builder> aRecordFuture = dnsClient.resolveA(apexDomain)
      .compose(
        aRecords -> {
          if (aRecords.isEmpty()) {
            return Future.failedFuture(aRecordsValidCheck.setMessage("The apex domain (" + apexDomain + ") has no A records"));
          }
          return Future.succeededFuture(aRecordsValidCheck.setMessage("A records were found"));

        }
        , err -> Future.failedFuture(aRecordsValidCheck.setFatalError(err))
      );
    dnsChecksFutureTests.add(aRecordFuture);

    /**
     * Domain blocked?
     * (Not yet async)
     */
    ValidationTestResult.Builder blockListCheck = ValidationTest.BLOCK_LIST.createResultBuilder();
    List<DnsBlockListQueryHelper> dnsBlockLists = DnsBlockListQueryHelper.forDomain(apexDomain).addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG)
      .build();
    for (DnsBlockListQueryHelper dnsBlockListQueryHelper : dnsBlockLists) {
      DnsName dnsNameToQuery = dnsBlockListQueryHelper.getDnsNameToQuery();
      Future<ValidationTestResult.Builder> futureBlockListCheck = dnsClient.resolveA(dnsNameToQuery)
        .compose(dnsIps -> {

          if (dnsIps.isEmpty()) {
            return Future.succeededFuture(blockListCheck.setMessage("The apex domain (" + apexDomain + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList()));
          }

          DnsIp dnsIp = dnsIps.iterator().next();
          DnsBlockListResponseCode dnsBlockListResponse = dnsBlockListQueryHelper.createResponseCode(dnsIp);
          if (dnsBlockListResponse.getBlocked()) {
            return Future.failedFuture(blockListCheck.setMessage("The apex domain (" + apexDomain + ") is blocked by " + dnsBlockListQueryHelper.getBlockList()));
          }

          return Future.succeededFuture(blockListCheck.setMessage("The apex domain (" + apexDomain + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList()));
        }, err -> Future.failedFuture(blockListCheck.setFatalError(err)));
      dnsChecksFutureTests.add(futureBlockListCheck);
    }


    /**
     * DNS Checks Composite execution
     */
    CompositeFuture compositeFuture;
    if (failEarly) {
      compositeFuture = Future.all(dnsChecksFutureTests);
    } else {
      compositeFuture = Future.join(dnsChecksFutureTests);
    }
    return compositeFuture
      .compose(
        results ->
          // the dns checks futures have all succeeded
          Future.succeededFuture(
            domainValidatorResult.addTests(results
              .list()
              .stream()
              .map(e -> ((ValidationTestResult.Builder) e).succeed())
              .collect(Collectors.toSet())
            ))
        ,
        err -> {
          if (!(err instanceof ValidationTestResult.Builder)) {
            return Future.failedFuture(err);
          }
          // one the future has not succeeded
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
      )
      .compose(v -> {
        // All DNS checks Records test have run
        // if the A record was positive, we can check the home page and the certificate of the web server
        boolean aRecordsResultHasPassed;
        try {
          aRecordsResultHasPassed = domainValidatorResult.getResult(ValidationTest.A_RECORD).hasPassed();
        } catch (NotFoundException e) {
          // should not but yeah
          aRecordsResultHasPassed = false;
        }
        Future<ValidationTestResult.Builder> homePageCheckBuilderFuture;
        if (aRecordsResultHasPassed) {
          homePageCheckBuilderFuture = this.getHomePageFutureCheck(apexDomain);
        } else {
          homePageCheckBuilderFuture = Future.failedFuture(
            ValidationTest.WEB_SERVER.createResultBuilder()
              .setMessage("No A records, web server and home page checks skipped.")
              .skipped()
          );
        }
        return homePageCheckBuilderFuture;
      })
      .compose(
        homePageCheckBuilderResult -> {
          // the home page check has succeeded
          ValidationTestResult homePageCheckResult = homePageCheckBuilderResult.succeed();
          domainValidatorResult.addTest(homePageCheckResult);
          return Future.succeededFuture(domainValidatorResult);
        },
        err -> {
          if (!(err instanceof ValidationTestResult.Builder)) {
            return Future.failedFuture(err);
          }
          ValidationTestResult homePageCheckResult = ((ValidationTestResult.Builder) err).fail();
          domainValidatorResult.addTest(homePageCheckResult);
          return Future.succeededFuture(domainValidatorResult);
        });
  }

  /**
   * HTML page test
   * (Certificate)
   *
   */
  private Future<ValidationTestResult.Builder> getHomePageFutureCheck(DnsName apexDomain) {

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

    return this.getHomePageResponse(apexDomain)
      .compose(response -> {
          String html = response.bodyAsString();
          try {
            HtmlGrading.grade(html, apexDomain);
            return Future.succeededFuture(homePage.setMessage("HTML page legit at (" + apexDomain + ")"));
          } catch (HtmlStructureException e) {
            return Future.failedFuture(homePage.setMessage("The HTML page (" + apexDomain + ") is not legit" + e.getMessage()));
          }
        },
        err -> {
          String message = "The web server (http(s)://" + apexDomain + ") has an error.";

          Set<Class<?>> nonFatalError = new HashSet<>();
          // Bad certificate
          nonFatalError.add(SSLHandshakeException.class);
          // Default throwable when the future is failed
          nonFatalError.add(NoStackTraceThrowable.class);
          /**
           * Fatal error example due to network that could be solved when run twice
           * io.vertx.core.http.HttpClosedException
           * java.net.UnknownHostException
           * io.netty.channel.ConnectTimeoutException
           */
          if (nonFatalError.contains(err.getClass())) {
            return Future.failedFuture(webServer.setNonFatalError(err, message));
          }
          return Future.failedFuture(webServer.setFatalError(err, message));
        }
      );
  }

  private Future<HttpResponse<Buffer>> getHomePageResponse(DnsName apexDomain) {

    /**
     * HTTPS with redirect first (https://vertx.io/docs/vertx-core/java/#_30x_redirection_handling)
     */
    String httpsUri = "https://" + apexDomain.toStringWithoutRoot();
    return webClient.getAbs(httpsUri)
      .followRedirects(true)
      .send()
      .compose(response -> {
          try {
            this.verifyRequestStatusCode(response);
          } catch (AssertionException e) {
            return Future.failedFuture(e);
          }
          return Future.succeededFuture(response);
        },
        httpsErr -> {
          /**
           * Try a redirection on http
           * to see if we land on a HTTPS url (example: ntlworld.com, orange.fr)
           */
          String httpUri = "http://" + apexDomain.toStringWithoutRoot();
          return webClient.getAbs(httpUri)
            .followRedirects(true)
            .send()
            .compose(
              request -> {
                try {
                  this.verifyRequestStatusCode(request);
                } catch (AssertionException e) {
                  return Future.failedFuture(e);
                }
                List<String> followedRedirects = request.followedRedirects();
                if (followedRedirects.isEmpty()) {
                  return Future.failedFuture("The domain (" + apexDomain + ") is only on http, not on https");
                }
                String lastRedirect = followedRedirects.get(followedRedirects.size() - 1);
                if (!lastRedirect.startsWith("https")) {
                  return Future.failedFuture("The domain (" + httpUri + ") does not redirect to a HTTPS URI. Redirect URI: " + lastRedirect);
                }
                return Future.succeededFuture(request);
              },
              // return the https error
              httpErr -> Future.failedFuture(httpsErr)
            );
        });
  }

  private void verifyRequestStatusCode(HttpResponse<Buffer> response) throws AssertionException {
    int statusCode = response.statusCode();
    if (statusCode != 200) {
      throw new AssertionException("The server returns a bad status (" + statusCode + ")");
    }
  }
}
