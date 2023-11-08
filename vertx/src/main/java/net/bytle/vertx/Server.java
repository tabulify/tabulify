package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoSecretException;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Properties, configuration and capabilities for a server (net/http)
 * <p>
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
  private IpGeolocation ipGeolocation;
  private JdbcSchemaManager jdbcManager;
  private JwtAuthManager jwtAuthManager;
  private ApiKeyAuthenticationProvider apiKeyAuth;
  private HashId hashId;
  private JacksonMapperManager jacksonMapperManager;

  Server(builder builder) {

    this.builder = builder;

  }

  /**
   * @param name - the name is used as prefix for the configuration (ie with the http name, the conf key are `http.host, http.port, ...`)
   */
  public static builder create(String name, Vertx vertx, ConfigAccessor configAccessor) {
    return new builder(name, vertx, configAccessor);
  }

  /**
   * @return a test server conf with port randomly picked.
   */
  public static JsonObject getTestServerConf(String serverName) throws IOException {

    ServerSocket socket = new ServerSocket(0);
    int testVerticlePort = socket.getLocalPort();
    socket.close();
    return new JsonObject().put(serverName + "." + LISTENING_PORT, testVerticlePort);
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


  public Vertx getVertx() {
    return this.builder.vertx;
  }

  public ConfigAccessor getConfigAccessor() {
    return this.builder.configAccessor;
  }

  public PgPool getJdbcPool() {
    if (this.jdbcPool == null) {
      throw new InternalException("No Jdbc Pool for the server");
    }
    return this.jdbcPool;
  }

  public IpGeolocation getIpGeolocation() {
    if (this.ipGeolocation == null) {
      throw new InternalException("No IpGeolocation configured for the server");
    }
    return this.ipGeolocation;
  }

  public JwtAuthManager getJwtAuth() {
    if (this.jwtAuthManager == null) {
      throw new InternalException("Jwt is not enabled for the server");
    }
    return this.jwtAuthManager;
  }

  public ApiKeyAuthenticationProvider getApiKeyAuth() {
    if (this.apiKeyAuth == null) {
      throw new InternalException("No API Key configured for the server");
    }
    return this.apiKeyAuth;
  }

  public HashId getHashId() {
    return this.hashId;
  }

  public JacksonMapperManager getJacksonMapperManager() {
    return this.jacksonMapperManager;
  }


  public static class builder {
    private final String name;
    private final Vertx vertx;
    private final ConfigAccessor configAccessor;
    private int listeningPort;
    private int publicPort;
    private String listeningHost;
    private Boolean ssl = false;
    private String poolName;
    private boolean enableIpGeoLocation = false;
    private boolean addJwt = false;
    private boolean addApiKeyAuth = false;
    private boolean enableHashId = false;
    private boolean enableJacksonTime = true;

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

    @SuppressWarnings("unused")
    public builder setListeningPort(int listeningPort) {
      this.listeningPort = listeningPort;
      return this;
    }

    @SuppressWarnings("unused")
    public builder setPublicPort(int publicPort) {
      this.publicPort = publicPort;
      return this;
    }

    public Server build() throws IllegalConfiguration {
      Server server = new Server(this);
      if (this.poolName != null) {
        LOGGER.info("Start creation of JDBC Pool (" + this.poolName + ")");
        server.jdbcConnectionInfo = JdbcConnectionInfo.createFromJson(this.poolName, server.getConfigAccessor());
        server.jdbcPool = JdbcPostgresPool.create(server.getVertx(), server.jdbcConnectionInfo);
        server.jdbcManager = JdbcSchemaManager.create(server.jdbcConnectionInfo);
      }
      if (this.enableIpGeoLocation) {
        try {
          server.ipGeolocation = IpGeolocation.create(server.jdbcPool, server.jdbcManager);
        } catch (DbMigrationException e) {
          throw new IllegalConfiguration("Ip geolocation bad schema migration", e);
        }
      } else {
        LOGGER.info("IP Geo-location not enabled");
      }
      if (this.addJwt) {
        try {
          server.jwtAuthManager = JwtAuthManager.create(server);
        } catch (NoSecretException e) {
          throw new IllegalConfiguration("Unable to init JWT", e);
        }
      } else {
        LOGGER.info("Jwt not enabled");
      }
      if (this.addApiKeyAuth) {
        server.apiKeyAuth = new ApiKeyAuthenticationProvider(server.getConfigAccessor());
      }
      if (this.enableHashId) {
        server.hashId = new HashId(server.getConfigAccessor());
      }

        server.jacksonMapperManager = JacksonMapperManager.create();
      if (this.enableJacksonTime) {
        server.jacksonMapperManager.enableTimeModuleForVertx();
      } else {
        LOGGER.info("Jackson time not enabled for vertx");
      }
      return server;
    }

    /**
     * @param name - the name is used in the configuration as prefix
     */
    public builder enableJdbcPool(String name) {
      this.poolName = name;
      return this;
    }

    public Server.builder addIpGeolocation() {
      if (this.poolName == null) {
        throw new InternalException("To enable Ip Geolocation, the jdbc pool service should be enabled first");
      }
      this.enableIpGeoLocation = true;
      return this;
    }

    /**
     * Add a JWT manager to create JWT token and authentice
     */
    @SuppressWarnings("unused")
    public Server.builder enableJwt() {
      this.addJwt = true;
      return this;
    }

    /**
     * Add an authentication via an API key
     * (ie token/session id)
     * and add a superuser token functionality
     */
    public Server.builder enableApiKeyAuth() {
      this.addApiKeyAuth = true;
      return this;
    }

    /**
     * Disable jackson time handling
     */
    @SuppressWarnings("unused")
    public Server.builder disableJacksonTime() {
      this.enableJacksonTime = false;
      return this;
    }

    /**
     * Enable the HashId utility
     */
    public Server.builder enableHashId() {
      this.enableHashId = true;
      return this;
    }
  }
}
