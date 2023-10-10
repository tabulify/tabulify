package net.bytle.monitor;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;

/**
 * Token expiration and ip filtering
 */
public class MonitorApiToken {
  private static final String API_TOKEN_CLOUDFLARE = "api.token.cloudflare";

  private static final String API_TOKEN_EXPIRATION = "api.token.days.before.expiration.failure";
  private static final Logger LOGGER = LogManager.getLogger(MonitorApiToken.class);
  private final long expirationDelayBeforeFailure;
  private final Vertx vertx;

  /**
   * Cloudflare:
   * To create an API token, you need to use a template (custom permission does not work)
   * to get access to
   * <a href="https://api.cloudflare.com/client/v4/user/tokens">...</a>
   * 2023-10-10: It does not work with custom permissions
   **/
  private final String cloudflareApiBearer;

  public MonitorApiToken(Vertx vertx, ConfigAccessor configAccessor) throws ConfigIllegalException {
    this.vertx = vertx;
    this.cloudflareApiBearer = configAccessor.getString(API_TOKEN_CLOUDFLARE);
    if (this.cloudflareApiBearer == null) {
      throw new ConfigIllegalException("The config variable " + configAccessor.getPossibleVariableNames(API_TOKEN_CLOUDFLARE) + " was not found");
    }
    LOGGER.info("API token cloudflare found");
    this.expirationDelayBeforeFailure = configAccessor.getLong(API_TOKEN_EXPIRATION, 60L);
    LOGGER.info("API token expiration warning set to " + this.expirationDelayBeforeFailure);

  }

  public static MonitorApiToken create(Vertx vertx, ConfigAccessor configAccessor) throws ConfigIllegalException {
    return new MonitorApiToken(vertx, configAccessor);
  }

  public Future<MonitorReport> check() {

    WebClient client = WebClient.create(vertx);
    return client
      .getAbs("https://api.cloudflare.com/client/v4/user/tokens")
      .putHeader("Authorization", "Bearer " + this.cloudflareApiBearer)
      .putHeader("Content-Type", "application/json")
      .send()
      .compose(response -> {

        MonitorReport monitorReport = new MonitorReport();

        String body = response.bodyAsString();
        JsonObject jsonBody;
        try {
          jsonBody = new JsonObject(body);
        } catch (Exception e) {
          return Future.failedFuture(new IllegalStateException("Content is not Json\n" + body, e));
        }


        JsonArray jsonArray = jsonBody.getJsonArray("result");
        for (int i = 0; i < jsonArray.size(); i++) {

          JsonObject tokenJsonData = jsonArray.getJsonObject(i);
          String id = tokenJsonData.getString("id");
          String name = tokenJsonData.getString("name");
          String status = tokenJsonData.getString("status");
          if (!status.equals("active")) {
            monitorReport.addFailure("The cloudflare api token (" + name + "," + id + "+) is not active");
            continue;
          }
          Instant expiresOn = tokenJsonData.getInstant("expires_on");
          if (expiresOn == null) {
            monitorReport.addSuccess("The cloudflare api token (" + name + ") has no expiration date");
            continue;
          }
          long duration = Duration.between(Instant.now(), expiresOn).toDays();
          String expirationMessage = "The cloudflare api token (" + name + ") expires in " + duration + " days";
          if (duration < expirationDelayBeforeFailure) {
            monitorReport.addFailure(expirationMessage);
            continue;
          }
          monitorReport.addSuccess(expirationMessage);

        }

        return Future.succeededFuture(monitorReport);
      });

  }


}
