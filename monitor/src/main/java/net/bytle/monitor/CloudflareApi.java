package net.bytle.monitor;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CloudflareApi {

  public static Logger LOGGER = LogManager.getLogger(CloudflareApi.class);
  private static final String API_TOKEN_CLOUDFLARE = "api.token.cloudflare";
  /**
   * APIToken
   * <p>
   * Important: for the control of token:
   * To create an API token, you need to use a template (custom permission does not work)
   * to get access to
   * <a href="https://api.cloudflare.com/client/v4/user/tokens">...</a>
   * 2023-10-10: It does not work with custom permissions
   * <p>
   */
  private final String cloudflareApiBearer;
  private final WebClient webClient;

  public CloudflareApi(WebClient webClient, ConfigAccessor configAccessor) throws ConfigIllegalException {
    this.cloudflareApiBearer = configAccessor.getString(API_TOKEN_CLOUDFLARE);
    if (this.cloudflareApiBearer == null) {
      throw new ConfigIllegalException("The config variable " + configAccessor.getPossibleVariableNames(API_TOKEN_CLOUDFLARE) + " was not found");
    }
    LOGGER.info("API token cloudflare found");
    this.webClient = webClient;
  }

  public static CloudflareApi create(WebClient webClient, ConfigAccessor configAccessor) throws ConfigIllegalException {
    return new CloudflareApi(webClient, configAccessor);
  }

  public HttpRequest<Buffer> getRequest(String absoluteUri) {
    return this.webClient
      .getAbs(absoluteUri)
      .putHeader("Authorization", "Bearer " + this.cloudflareApiBearer)
      .putHeader("Content-Type", "application/json");
  }

}
