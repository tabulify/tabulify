package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * An internal utility class to build Vertx HttpClient or WebClient
 */
public class HttpClientBuilder {

  public HttpClientBuilder(Vertx vertx) {
    this.vertx = vertx;
  }

  public static HttpClientBuilder builder(Vertx vertx) {
    return new HttpClientBuilder(vertx);
  }


  private final Vertx vertx;
  private ServerProperties serverProperties;
  private ProxyOptions proxyOptions;
  private Integer defaultPort;
  private String defaultHost;


  public HttpClientBuilder withServerProperties(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
    return this;
  }

  public HttpClientBuilder setProxyOptions(ProxyOptions proxyOptions) {
    this.proxyOptions = proxyOptions;
    return this;
  }

  public WebClient buildWebClient() {

    WebClientOptions webClientOptions = new WebClientOptions(getHttpClientOptions());
    return WebClient.create(this.vertx, webClientOptions);
  }

  public HttpClient buildHttpClient() {
    return this.vertx.createHttpClient(getHttpClientOptions());
  }

  public HttpClientOptions getHttpClientOptions() {

    /**
     * Http Clients Options
     */
    HttpClientOptions httpClientOptions = new HttpClientOptions();

    /**
     * Enable Ssl
     * If you want to remove the `trustAll=true`, you need
     * to add the certificate on the OS and not using the {@link HttpClientOptions#setPemTrustOptions(PemTrustOptions)}
     * otherwise it will trust only this Root certificate.
     * <p>
     * See: https://groups.google.com/g/vertx/c/NYLcHzY8EYM
     */
    if (this.serverProperties.getSsl()) {
      httpClientOptions
        .setSsl(true)
        .setTrustAll(true);
    }

    /**
     * Target Host and port
     */
    Integer port = this.defaultPort != null ? defaultPort : this.serverProperties.getListeningPort();
    httpClientOptions.setDefaultPort(port);
    String host = this.defaultHost != null ? this.defaultHost : "localhost";
    httpClientOptions.setDefaultHost(host);


    if (this.proxyOptions != null) {
      httpClientOptions.setProxyOptions(proxyOptions);
    }

    /**
     * TCP Timeout
     */
    //httpClientOptions.setConnectTimeout(100); // ms
    //httpClientOptions.setIdleTimeout(1); // second, a normal answer is of 22 ms
    //httpClientOptions.setKeepAlive(true);

    /**
     * Http client
     */
    return httpClientOptions;

  }

  public HttpClientBuilder setDefaultPort(Integer defaultPort) {
    this.defaultPort = defaultPort;
    return this;
  }

  public HttpClientBuilder setDefaultHost(String defaultHost) {
    this.defaultHost = defaultHost;
    return this;
  }


}
