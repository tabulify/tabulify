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

  private Integer maxHeaderSize;
  private Integer connectTimeOut;

  public HttpClientBuilder(Vertx vertx) {
    this.vertx = vertx;
  }

  public static HttpClientBuilder builder(Vertx vertx) {
    return new HttpClientBuilder(vertx);
  }


  private final Vertx vertx;
  private Server server;
  private ProxyOptions proxyOptions;
  private Integer defaultPort;
  private String defaultHost;


  public HttpClientBuilder withServerProperties(Server server) {
    this.server = server;
    return this;
  }

  public HttpClientBuilder setProxyOptions(ProxyOptions proxyOptions) {
    this.proxyOptions = proxyOptions;
    return this;
  }

  public WebClient buildWebClient() {

    WebClientOptions webClientOptions = new WebClientOptions(buildHttpClientOptions());
    return WebClient.create(this.vertx, webClientOptions);
  }

  public HttpClient buildHttpClient() {
    return this.vertx.createHttpClient(buildHttpClientOptions());
  }

  private HttpClientOptions buildHttpClientOptions() {

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
    boolean ssl = this.server != null ? this.server.getSsl() : false;
    if (ssl) {
      httpClientOptions
        .setSsl(true)
        .setTrustAll(true);
    }

    /**
     * Target Host and port
     */
    int port = this.defaultPort != null ? defaultPort : (this.server != null ? this.server.getListeningPort() : 80);
    httpClientOptions.setDefaultPort(port);
    String host = this.defaultHost != null ? this.defaultHost : "localhost";
    httpClientOptions.setDefaultHost(host);


    if (this.proxyOptions != null) {
      httpClientOptions.setProxyOptions(proxyOptions);
    }

    if (this.maxHeaderSize != null) {
      httpClientOptions.setMaxHeaderSize(this.maxHeaderSize);
    }

    /**
     * TCP Timeout
     */
    if (this.connectTimeOut != null) {
      httpClientOptions.setConnectTimeout(this.connectTimeOut); // ms
    }
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

  /**
   * The maximum length of the HTTP headers.
   * This is a restriction in cookie length, including number of cookies and size of cookie values.
   * Default: 8192
   */
  public HttpClientBuilder setMaxHeaderSize(int maxHeaderSize) {
    this.maxHeaderSize = maxHeaderSize;
    return this;
  }

  /**
   * @param connectTimeout en microseconds
   */
  public HttpClientBuilder setConnectTimeout(int connectTimeout) {
    this.connectTimeOut = connectTimeout;
    return this;
  }

}
