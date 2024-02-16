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
  private final Set<String> whiteListApexDomains;
  private final Vertx vertx;
  private final Boolean checkPingMx;

  /**
   * Grey List are not allowed on import
   * but via email validation
   */
  private final HashSet<String> greyListApexDomains;
  private final HashSet<String> blockListApexDomains;


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
    whiteListApexDomains = new HashSet<>();
    whiteListApexDomains.add("aol.com");
    whiteListApexDomains.add("bigmir.net"); // ukrainian portal
    whiteListApexDomains.add("unam.mx"); // it's not an apex domain but is a university student
    whiteListApexDomains.add("free.fr");
    whiteListApexDomains.add("hotmail.com");
    whiteListApexDomains.add("icloud.com");
    whiteListApexDomains.add("gmail.com");
    whiteListApexDomains.add("gmx.com");
    whiteListApexDomains.add("googlemail.com");
    whiteListApexDomains.add("outlook.com");
    whiteListApexDomains.add("live.com");
    whiteListApexDomains.add("mail.com");
    whiteListApexDomains.add("mail.ru"); // mail.ru: https://en.wikipedia.org/wiki/VK_(company)
    whiteListApexDomains.add("orange.fr"); // mail.ru: https://en.wikipedia.org/wiki/VK_(company)
    whiteListApexDomains.add("protonmail.com");
    whiteListApexDomains.add("wanadoo.fr");
    whiteListApexDomains.add("yahoo.com");
    whiteListApexDomains.add("yandex.com");
    whiteListApexDomains.add("yandex.ru");
    whiteListApexDomains.add("zoho.com");

    /**
     * The known blocked domain list that we don't test
     */
    greyListApexDomains = new HashSet<>();
    greyListApexDomains.add("163.com");

    /**
     * Blocking
     */
    blockListApexDomains = new HashSet<>();
    blockListApexDomains.add("backlinksgenerator.in");
    blockListApexDomains.add("horsetipstersreview.com"); // betting platform
    blockListApexDomains.add("tremunpiercing.com"); // 3 email prueba3@..., prueba4@..., prueba5@...


  }

  /**
   * @param dnsNameToCheck   - the domain to validate
   * @param failEarly - if true, will stop to the first failure and return it
   * @return a list of validation
   */
  public Future<DomainValidatorResult> validate(DnsName dnsNameToCheck, boolean failEarly) {

    DomainValidatorResult domainValidatorResult = new DomainValidatorResult();

    /**
     * White List / Legit Apex Domains
     */
    DnsName apexDomain = dnsNameToCheck.getApexName();
    String apexDomainNameOnList = apexDomain.toStringWithoutRoot().toLowerCase();
    if (whiteListApexDomains.contains(apexDomainNameOnList)) {
      domainValidatorResult.addTestResult(ValidationTest.WHITE_LIST.createResultBuilder()
        .setMessage("Domain (" + apexDomainNameOnList + ") is on the white list")
        .succeed());
      return Future.succeededFuture(domainValidatorResult);
    }

    if (greyListApexDomains.contains(apexDomainNameOnList)) {
      domainValidatorResult.addTestResult(
        ValidationTest.GREY_LIST.createResultBuilder()
          .setMessage("Domain (" + apexDomainNameOnList + ") is on the grey list")
          .fail()
      );
      return Future.succeededFuture(domainValidatorResult);
    }
    if (blockListApexDomains.contains(apexDomainNameOnList)) {
      domainValidatorResult.addTestResult(
        ValidationTest.BLOCK_LIST.createResultBuilder()
          .setMessage("Domain (" + apexDomainNameOnList + ") is on the block list")
          .fail()
      );
      return Future.succeededFuture(domainValidatorResult);
    }

    /**
     * A future for all checks
     */
    List<Future<Void>> checksFutureTests = new ArrayList<>();

    /**
     * Dns Check: The domain name should have Mx record
     */
    checksFutureTests.add(this.mxChecks(dnsNameToCheck, domainValidatorResult));

    /**
     * Dns Check: Domain blocked?
     * (Not yet async)
     */
    checksFutureTests.add(this.externalBlockLists(dnsNameToCheck, domainValidatorResult));

    /**
     * Home page Checks (A record + HTML page)
     */
    checksFutureTests.add(this.getHomePageFutureCheck(dnsNameToCheck, domainValidatorResult));

    /**
     * DNS Checks Composite execution
     */
    CompositeFuture compositeFuture;
    if (failEarly) {
      compositeFuture = Future.all(checksFutureTests);
    } else {
      compositeFuture = Future.join(checksFutureTests);
    }
    return compositeFuture
      .compose(results -> Future.succeededFuture(domainValidatorResult));

  }

  private Future<Void> externalBlockLists(DnsName dnsName, DomainValidatorResult domainValidatorResult) {

    ValidationTestResult.Builder blockListTestBuilder = ValidationTest.BLOCK_LIST.createResultBuilder();
    /**
     * For now, there is only one block list wherw we test
     */
    DnsBlockListQueryHelper dnsBlockListQueryHelper = DnsBlockListQueryHelper
      .forDomain(dnsName).addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG)
      .build()
      .get(0);

    return dnsClient.resolveA(dnsBlockListQueryHelper.getDnsNameToQuery())
      .compose(dnsIps -> {
        if (dnsIps.isEmpty()) {
          domainValidatorResult.addTestResult(
            blockListTestBuilder
              .setMessage("The apex domain (" + dnsName + ") is not blocked by " + dnsBlockListQueryHelper.getBlockList())
              .succeed()
          );
          return Future.succeededFuture();
        }
        DnsIp dnsIp = dnsIps.iterator().next();
        DnsBlockListResponseCode dnsBlockListResponse = dnsBlockListQueryHelper.createResponseCode(dnsIp);
        if (dnsBlockListResponse.getBlocked()) {
          domainValidatorResult.addTestResult(
            blockListTestBuilder
              .setMessage("The apex domain (" + dnsName + ") is blocked by " + dnsBlockListQueryHelper.getBlockList())
              .fail()
          );
          return Future.succeededFuture();
        }
        return Future.succeededFuture();
      }, err -> {
        domainValidatorResult.addTestResult(blockListTestBuilder.setFatalError(err).fail());
        return Future.succeededFuture();
      });

  }

  /**
   * Test the MX of a DNS name
   * @param dnsNameToCheck - the DNS name to check
   * @param domainValidatorResult - the validator object result
   */
  private Future<Void> mxChecks(DnsName dnsNameToCheck, DomainValidatorResult domainValidatorResult) {
    ValidationTestResult.Builder mxValidCheckTestBuilder = ValidationTest.MX_RECORD.createResultBuilder();
    return dnsClient.resolveMx(dnsNameToCheck)
      .compose(records -> {
        String noMxRecordMessage = "The domain (" + dnsNameToCheck + ") has no MX records";
        if (records.isEmpty()) {
          domainValidatorResult.addTestResult(mxValidCheckTestBuilder.setMessage(noMxRecordMessage).fail());
          return Future.succeededFuture();
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
                    .pingHello();
                } catch (MessagingException e) {
                  smtpException = e;
                }
              }
              return smtpException;
            })
            .compose(smtpError -> {
              String message = "Mx records were found";
              ValidationTestResult result;
              if (smtpError == null) {
                if (this.checkPingMx) {
                  message += " and smtp servers pinged (" + mxLists + ")";
                } else {
                  message += " and smtp servers ping is disabled (" + mxLists + ")";
                }
                result = mxValidCheckTestBuilder.setMessage(message).succeed();
              } else {
                result = mxValidCheckTestBuilder.setFatalError(smtpError, message + " but no smtp server has responded (" + mxLists + ")").fail();
              }
              domainValidatorResult.addTestResult(result);
              return Future.succeededFuture();
            });
        }
      }, err -> {
        domainValidatorResult.addTestResult(mxValidCheckTestBuilder.setFatalError(err).fail());
        return Future.succeededFuture();
      });
  }

  /**
   * Test the HOME HTML page of a domain
   * (A record, Certificate, Page content)
   */
  private Future<Void> getHomePageFutureCheck(DnsName dnsName, DomainValidatorResult domainValidatorResult) {

    /**
     * These tests have dependencies
     * * A home page cannot be tested if the web server does not have any a record
     * or does not respond
     * * A web server cannot be tested if the web server does not have any a record
     */
    ValidationTestResult.Builder aRecordsValidCheck = ValidationTest.A_RECORD.createResultBuilder();
    ValidationTestResult.Builder webServer = ValidationTest.WEB_SERVER.createResultBuilder();
    ValidationTestResult.Builder homePage = ValidationTest.HOME_PAGE.createResultBuilder();

    /**
     * The `A` record DNS check
     */
    return dnsClient.resolveA(dnsName)
      .compose(
        aRecords -> {
          if (aRecords.isEmpty()) {
            domainValidatorResult.addTestResult(aRecordsValidCheck.setMessage("The apex domain (" + dnsName + ") has no A records").fail());
            domainValidatorResult.addTestResult(webServer.setMessage("No A record were found for (" + dnsName + ")").skipped());
            domainValidatorResult.addTestResult(homePage.setMessage("No A record were found for (" + dnsName + ")").skipped());
            return Future.succeededFuture();
          }
          domainValidatorResult.addTestResult(aRecordsValidCheck.setMessage("A records were found").succeed());
          return Future.succeededFuture();
        }
        , err -> {
          /**
           * We don't go further
           */
          aRecordsValidCheck.setFatalError(err);
          webServer.setMessage("Fatal Error on A records").skipped();
          homePage.setMessage("Fatal Error on A records").skipped();
          return Future.failedFuture(aRecordsValidCheck.setFatalError(err));
        }
      )
      .compose(v -> {
        /**
         * We check the web server only if the A test has succeeded
         */
        return this.getHomePageResponse(dnsName)
          .compose(response -> {
              domainValidatorResult.addTestResult(webServer.setMessage("The web server is legit").succeed());
              String html = response.bodyAsString();
              try {
                HtmlGrading.grade(html, dnsName);
                domainValidatorResult.addTestResult(homePage.setMessage("HTML page legit at (" + dnsName + ")").succeed());
                return Future.succeededFuture();
              } catch (HtmlStructureException e) {
                domainValidatorResult.addTestResult(homePage.setMessage("The HTML page (" + dnsName + ") is not legit" + e.getMessage()).fail());
                return Future.succeededFuture();
              }
            },
            err -> {
              String message = "The web server (http(s)://" + dnsName + ") has an error.";

              Set<Class<?>> nonFatalError = new HashSet<>();
              // Bad certificate
              nonFatalError.add(SSLHandshakeException.class);
              // Default throwable when the future is failed
              nonFatalError.add(NoStackTraceThrowable.class);

              /**
               * Skipped test
               */
              domainValidatorResult.addTestResult(homePage.setMessage("Web server test failed").skipped());

              /**
               * Fatal error example due to network that could be solved when run twice
               * io.vertx.core.http.HttpClosedException
               * java.net.UnknownHostException
               * io.netty.channel.ConnectTimeoutException
               */
              if (nonFatalError.contains(err.getClass())) {
                webServer.setNonFatalError(err, message);
              } else {
                webServer.setFatalError(err, message);
              }
              domainValidatorResult.addTestResult(webServer.fail());
              return Future.succeededFuture();
            }
          );
      });

  }

  private Future<HttpResponse<Buffer>> getHomePageResponse(DnsName dnsName) {

    /**
     * HTTPS with redirect first (https://vertx.io/docs/vertx-core/java/#_30x_redirection_handling)
     */
    String httpsUri = "https://" + dnsName.toStringWithoutRoot();
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
          String httpUri = "http://" + dnsName.toStringWithoutRoot();
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
                  return Future.failedFuture("The domain (" + dnsName + ") is only on http, not on https");
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
