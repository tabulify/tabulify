package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import net.bytle.exception.InternalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Properties for a net/http server
 * <p>
 * An HTTP server object:
 * * to read
 * * or create a Json Config pass for easy testing
 */
public class Server {

  static Logger LOGGER = LogManager.getLogger(Server.class);

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
  private PgPool jdbcPool;
  private JdbcConnectionInfo jdbcConnectionInfo;


  Server(builder builder) {

    this.builder = builder;

  }

  /**
   * @param name - the name is used as prefix for the configuration (ie with the http name, the conf key are `http.host, http.port, ...`)
   */
  public static builder create(String name, Vertx vertx, ConfigAccessor configAccessor) {
    return new builder(name, vertx, configAccessor);
  }


  @Override
  public String toString() {
    return builder.name;
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

  public Vertx getVertx() {
    return this.builder.vertx;
  }

  public ConfigAccessor getConfigAccessor() {
    return this.builder.configAccessor;
  }

  public JdbcSchemaManager getJdbcManager() {
    if (jdbcConnectionInfo == null) {
      throw new InternalException("No Jdbc Pool for the server");
    }
    return JdbcSchemaManager.create(jdbcConnectionInfo);
  }

  public PgPool getJdbcPool() {
    if (this.jdbcPool == null) {
      throw new InternalException("No Jdbc Pool for the server");
    }
    return this.jdbcPool;
  }


  public static class builder {
    private final String name;
    private final Vertx vertx;
    private final ConfigAccessor configAccessor;
    private int listeningPort;
    private int publicPort;
    private String listeningHost;
    private Boolean ssl = false;
    private boolean addJdbcPool = false;

    public builder(String name, Vertx vertx, ConfigAccessor configAccessor) {
      this.name = name;
      this.vertx = vertx;
      this.configAccessor = configAccessor;
    }

    String getListeningHostKey() {
      return this.name + "." + Server.HOST;
    }

    public builder setFromConfigAccessorWithPort(int portDefault) {
      this.listeningHost = configAccessor.getString(getListeningHostKey(), WILDCARD_IPV4_ADDRESS);
      LOGGER.info("The listening host was set to: " + this.listeningHost + " via the conf (" + getListeningHostKey() + ")");
      this.listeningPort = configAccessor.getInteger(getListeningPortKey(), portDefault);
      LOGGER.info("The listening port was set to: " + this.listeningPort + " via the conf (" + getListeningPortKey() + ")");
      this.publicPort = configAccessor.getInteger(getPublicPortKey(), 80);
      LOGGER.info("The public port was set to: " + this.publicPort + " via the conf (" + getPublicPortKey() + ")");
      /**
       * Note that Chrome does not allow to set a third-party cookie (ie same site: None)
       * if the connection is not secure.
       * It must be true then everywhere.
       * For non-app, https comes from the proxy.
       */
      this.ssl = configAccessor.getBoolean(getSslKey(), true);
      LOGGER.info("SSL was set to: " + this.ssl + " via the conf (" + getSslKey() + ")");
      return this;
    }

    private String getSslKey() {
      return this.name + "." + Server.SSL;
    }

    private String getPublicPortKey() {
      return this.name + "." + Server.PUBLIC_PORT;
    }

    private String getListeningPortKey() {
      return this.name + "." + Server.LISTENING_PORT;
    }

    public builder setListeningPort(int listeningPort) {
      this.listeningPort = listeningPort;
      return this;
    }

    public builder setPublicPort(int publicPort) {
      this.publicPort = publicPort;
      return this;
    }

    public Server build() {
      Server server = new Server(this);
      if (this.addJdbcPool) {
        LOGGER.info("Start creation of JDBC Pool");
        server.jdbcConnectionInfo = JdbcConnectionInfo.createFromJson(server.getConfigAccessor());
        server.jdbcPool = JdbcPostgresPool.create(server.getVertx(), server.jdbcConnectionInfo);
      }
      return server;
    }

    public builder addJdbcPool() {
      this.addJdbcPool = true;
      return this;
    }
  }
}
