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
  private static final Logger LOGGER = LogManager.getLogger(MonitorApiToken.class);
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
  }

  public static MonitorApiToken create(Vertx vertx, ConfigAccessor configAccessor) throws ConfigIllegalException {
    return new MonitorApiToken(vertx, configAccessor);
  }

  public Future<Void> check() {

    WebClient client = WebClient.create(vertx);
    return client
      .getAbs("https://api.cloudflare.com/client/v4/user/tokens")
      .putHeader("Authorization", "Bearer " + this.cloudflareApiBearer)
      .putHeader("Content-Type", "application/json")
      .send()
      .onFailure(LOGGER::error)
      .compose(response -> {

        System.out.println("Received response with status code" + response.statusCode());
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
          Instant expiresOn = tokenJsonData.getInstant("expires_on");
          Duration duration = Duration.between(Instant.now(), expiresOn);
          System.out.println("The token (" + name + ") expires in " + duration.toDays() + " days");

        }

        return null;
      });


  }


  /**
   * (Certbot: Cloudflare, ...)
   * curl -X GET "https://api.cloudflare.com/client/v4/user/tokens/verify" \
   *      -H "Authorization: Bearer xxxxxx" \
   *      -H "Content-Type:application/json"
   *  https://developers.cloudflare.com/fundamentals/api/how-to/roll-token/
   *
   *
   * https://developers.cloudflare.com/api/operations/user-api-tokens-list-tokens
   * https://developers.cloudflare.com/api/operations/zone-level-access-service-tokens-list-service-tokens
   */

}
