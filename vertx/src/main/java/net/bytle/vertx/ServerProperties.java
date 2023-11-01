package net.bytle.vertx;

import io.vertx.core.json.JsonObject;

/**
 * Properties for a net/http server
 * <p>
 * An HTTP server object:
 * * to read
 * * or create a Json Config pass for easy testing
 */
public class ServerProperties {

  /**
   * Listen from all hostname
   * On ipv4 and Ipv6.
   * The wildcard implementation depends on the language
   * and in Java it works for the 2 Ip formats.
   */
  public static final String WILDCARD_IPV4_ADDRESS = "0.0.0.0";
  @SuppressWarnings("unused")
  public static final String WILDCARD_IPV6_ADDRESS = "[::]";
  /**
   * The default path for the key
   * See the https.md documentation for more info.
   */
  public static final String DEV_KEY_PEM = "cert/key.pem";
  public static final String DEV_CERT_PEM = "cert/cert.pem";
  // Because they are constants, the names of an enum type's fields are in uppercase letters.
  static String HOST = "host";
  static String LISTENING_PORT = "port"; // the private listening port
  static String PUBLIC_PORT = "port.public"; // the public port (the proxy port, normally 80)

  static String SSL = "ssl"; // SSL


  private final builder builder;


  ServerProperties(builder builder) {

    this.builder = builder;

  }

  /**
   * @param prefix - the key prefix to add a namespace (example: http )
   */
  public static builder create(String prefix) {
    return new builder(prefix);
  }


  @Override
  public String toString() {
    return builder.serverPrefix;
  }

  public String getListeningHost() {

    return this.builder.listeningHost;

  }

  public Integer getListeningPort() {
    return this.builder.listeningPort;
  }

  public Integer getPublicPort() {
    return this.builder.publicPort;
  }

  public Boolean getSsl() {
    return this.builder.ssl;
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put(this.builder.getListeningHostKey(), this.builder.listeningHost)
      .put(this.builder.getListeningPortKey(), this.builder.listeningPort)
      .put(this.builder.getPublicPortKey(), this.builder.publicPort)
      .put(this.builder.getSslKey(), this.builder.ssl);
  }

  public static class builder {
    private final String serverPrefix;
    private int listeningPort;
    private int publicPort;
    private String listeningHost;
    private Boolean ssl = false;

    public builder(String serverPrefix) {
      this.serverPrefix = serverPrefix;
    }

    String getListeningHostKey() {
      return this.serverPrefix + "." + ServerProperties.HOST;
    }

    public builder fromConfigAccessor(ConfigAccessor configAccessor, int portDefault) {
      this.listeningHost = configAccessor.getString(getListeningHostKey(), WILDCARD_IPV4_ADDRESS);
      this.listeningPort = configAccessor.getInteger(getListeningPortKey(), portDefault);
      this.publicPort = configAccessor.getInteger(getPublicPortKey(), 80);
      this.ssl = configAccessor.getBoolean(getSslKey(), false);
      return this;
    }

    private String getSslKey() {
      return this.serverPrefix+"."+ServerProperties.SSL;
    }

    private String getPublicPortKey() {
      return this.serverPrefix + "." + ServerProperties.PUBLIC_PORT;
    }

    private String getListeningPortKey() {
      return this.serverPrefix + "." + ServerProperties.LISTENING_PORT;
    }

    public builder setListeningPort(int listeningPort) {
      this.listeningPort = listeningPort;
      return this;
    }

    public builder setPublicPort(int publicPort) {
      this.publicPort = publicPort;
      return this;
    }

    public ServerProperties build() {
      return new ServerProperties(this);
    }
  }
}
