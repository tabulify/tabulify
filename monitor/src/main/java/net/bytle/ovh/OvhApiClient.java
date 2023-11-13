package net.bytle.ovh;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapted from
 * <a href="https://github.com/ovh/java-ovh/blob/master/src/main/java/com/ovh/api/OvhApi.java">...</a>
 */

public class OvhApiClient {

  static Logger LOGGER = LogManager.getLogger(OvhApiClient.class);
  private final Builder builder;


  public OvhApiClient(Builder builder) {

    this.builder = builder;

  }


  /**
   * @param prefixName - a configuration prefix and a name for this client
   * @param webClient  - the webClient
   */
  public static Builder builder(String prefixName, WebClient webClient) {
    return new Builder(prefixName, webClient);
  }


  public static String HashSHA1(String text) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] sha1hash;
    md.update(text.getBytes(StandardCharsets.ISO_8859_1), 0, text.length());
    sha1hash = md.digest();
    StringBuilder sb = new StringBuilder();
    for (byte hash : sha1hash) {
      sb.append(Integer.toString((hash & 0xff) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }


  public OvhRequestBuilder getRequest(String path) {

    return new OvhRequestBuilder()
      .setGet(path);


  }


  public static class Builder {
    private static final String APPLICATION_SECRET_CONF = "application.secret";
    private static final String APPLICATION_KEY_CONF = "application.key";
    private static final String APPLICATION_ENDPOINT_CONF = "endpoint";
    private static final String CONSUMER_KEY_CONF = "consumer.key";
    /**
     * Name is used as configuration prefix
     */
    final String name;
    private final WebClient webClient;

    String applicationEndpoint;
    String applicationSecret;
    String applicationKey;
    String consumerKey;

    private final static Map<String, String> endpoints;

    static {
      endpoints = new HashMap<>();
      endpoints.put("ovh-eu", "https://eu.api.ovh.com/1.0");
      endpoints.put("ovh-ca", "https://ca.api.ovh.com/1.0");
      endpoints.put("kimsufi-eu", "https://eu.api.kimsufi.com/1.0");
      endpoints.put("kimsufi-ca", "https://ca.api.kimsufi.com/1.0");
      endpoints.put("soyoustart-eu", "https://eu.api.soyoustart.com/1.0");
      endpoints.put("soyoustart-ca", "https://ca.api.soyoustart.com/1.0");
      endpoints.put("runabove", "https://api.runabove.com/1.0");
      endpoints.put("runabove-ca", "https://api.runabove.com/1.0");
    }

    public Builder(String name, WebClient webClient) {
      this.name = name;
      this.webClient = webClient;
    }

    public Builder withConfigAccessor(ConfigAccessor configAccessor) throws ConfigIllegalException {
      String applicationEndpointConf = this.name + "." + APPLICATION_ENDPOINT_CONF;
      String applicationEndpointShort = configAccessor.getString(applicationEndpointConf, "ovh-eu");
      this.applicationEndpoint = endpoints.get(applicationEndpointShort);
      if (this.applicationEndpoint == null) {
        throw new ConfigIllegalException("The endpoint was not found with the value (" + applicationEndpointShort + ") found via the configuration (" + applicationEndpointConf + ")");
      }
      LOGGER.info("Ovh Api Client (" + this + ") endpoint value is : " + this.applicationEndpoint);

      String applicationKeyConf = this.name + "." + APPLICATION_KEY_CONF;
      this.applicationKey = configAccessor.getString(applicationKeyConf);
      if (applicationKey == null) {
        throw new ConfigIllegalException("The configuration (" + configAccessor.getPossibleVariableNames(applicationEndpointConf) + ") was not found");
      }
      LOGGER.info("Ovh Api Client (" + this + ") application key was found");

      String applicationSecretConf = this.name + "." + APPLICATION_SECRET_CONF;
      this.applicationSecret = configAccessor.getString(applicationSecretConf);
      if (applicationSecret == null) {
        throw new ConfigIllegalException("The configuration (" + configAccessor.getPossibleVariableNames(applicationSecretConf) + ") was not found");
      }
      LOGGER.info("Ovh Api Client (" + this + ") application secret was found");

      String consumerKeyConf = this.name + "." + CONSUMER_KEY_CONF;
      this.consumerKey = configAccessor.getString(consumerKeyConf);
      if (consumerKey == null) {
        throw new ConfigIllegalException("The configuration (" + configAccessor.getPossibleVariableNames(consumerKeyConf) + ") was not found");
      }
      LOGGER.info("Ovh Api Client (" + this + ") consumer key was found");

      return this;

    }


    public OvhApiClient build() throws ConfigIllegalException {
      if (this.applicationEndpoint == null || this.applicationKey == null || this.applicationSecret == null || consumerKey == null) {
        throw new ConfigIllegalException("one of application key, endpoint, secret and consumer key is null");
      }
      return new OvhApiClient(this);
    }
  }

  /**
   * A fluent builder because the sign of the request should happen
   * before sending
   */
  public class OvhRequestBuilder {


    private HttpRequest<Buffer> request;

    @SuppressWarnings("SameParameterValue")
    private void signRequest(String body) {


      // get timestamp from local system
      long timestamp = System.currentTimeMillis() / 1000;
      // build signature
      String uri = "https://" + request.host() + request.uri();
      MultiMap queryParams = request.queryParams();
      if (queryParams.size() > 0) {
        uri += "?";
        uri += queryParams.entries()
          .stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining("&"));
      }


      String method = request.method().toString().toUpperCase();
      String toSign = builder.applicationSecret +
        "+" +
        builder.consumerKey +
        "+" +
        method +
        "+" +
        uri +
        "+" +
        body +
        "+" +
        timestamp;
      String signature = "$1$" + HashSHA1(toSign);

      // set HTTP headers for authentication
      request.putHeader("X-Ovh-Consumer", builder.consumerKey);
      request.putHeader("X-Ovh-Application", builder.applicationKey);
      request.putHeader("X-Ovh-Signature", signature);
      request.putHeader("X-Ovh-Timestamp", Long.toString(timestamp));

    }

    public OvhRequestBuilder setGet(String path) {
      request = builder.webClient.getAbs(builder.applicationEndpoint + path);
      request.putHeader("Content-Type", "application/json");
      return this;
    }

    public OvhRequestBuilder addQueryParam(String param, String value) {
      request.addQueryParam(param, value);
      return this;
    }

    public Future<HttpResponse<Buffer>> send() {
      signRequest( "");
      return request.send();
    }
  }
}
