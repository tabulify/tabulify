package net.bytle.vertx;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bytle.email.BMailSmtpConnectionParameters;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoSecretException;
import net.bytle.exception.NullValueException;
import net.bytle.vertx.analytics.AnalyticsTracker;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;
import net.bytle.vertx.collections.MapDb;
import net.bytle.vertx.collections.WriteThroughCollection;
import net.bytle.vertx.db.JdbcClient;
import net.bytle.vertx.db.JdbcPostgres;
import net.bytle.vertx.future.TowerFutures;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * A server represents a TCP/IP server
 * and offers multiple service that can be enabled/disabled
 * and used by apps.
 * It wraps a {@link Vertx object}
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
  public static final String DEV_KEY_PEM = "../cert/key.pem";
  public static final String DEV_CERT_PEM = "../cert/cert.pem";
  // Because they are constants, the names of an enum type's fields are in uppercase letters.
  static String HOST = "host";
  static String LISTENING_PORT = "port"; // the private listening port
  static String PUBLIC_PORT = "port.public"; // the public port (the proxy port, normally 80)

  static String SSL = "ssl"; // SSL


  private final builder builder;
  private JdbcClient pgDatabaseConnectionPool;
  private IpGeolocation ipGeolocation;
  private JwtAuthManager jwtAuthManager;
  private ApiKeyAuthenticationProvider apiKeyAuth;
  private HashId hashId;
  private JacksonMapperManager jacksonMapperManager;
  private JsonToken jsonToken;
  private TowerFailureHandler failureHandler;
  private AnalyticsTracker analyticsTracker;
  private TowerSmtpClientService smtpClient;
  private MapDb mapDb;
  /**
   * A list because the services should be started in order
   * (ie session authentication should be first on the router)
   */
  private final List<TowerService> services = new ArrayList<>();
  private TowerDnsClient dnsClient;
  private WriteThroughCollection writeThroughCollection;

  Server(builder builder) {

    this.builder = builder;

  }

  /**
   * @param name - the name is used as prefix for the server configuration (ie with the http name, the conf key are `http.host, http.port, ...`)
   *             as you can create more than one server listening
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

  /**
   * @return the listening port
   */
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

  public JdbcClient getPostgresClient() {
    if (this.pgDatabaseConnectionPool == null) {
      throw new InternalException("No Pg Jdbc Pool for the server");
    }
    return this.pgDatabaseConnectionPool;
  }

  @SuppressWarnings("unused")
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

  public ApiKeyAuthenticationProvider getApiKeyAuthProvider() throws NullValueException {
    if (this.apiKeyAuth == null) {
      throw new NullValueException("No API Key configured for the server");
    }
    return this.apiKeyAuth;
  }

  public HashId getHashId() {
    return this.hashId;
  }

  public JacksonMapperManager getJacksonMapperManager() {
    return this.jacksonMapperManager;
  }

  public JsonToken getJsonToken() {
    if (jsonToken == null) {
      throw new InternalException("The Json Token utility was not enabled for this server.");
    }
    return this.jsonToken;
  }

  public PrometheusMeterRegistry getMetricsRegistry() {
    return MainLauncher.prometheus.getRegistry();
  }


  public TowerFailureHandler getFailureHandler() {
    return this.failureHandler;
  }

  public AnalyticsTracker getTrackerAnalytics() {
    if (this.analyticsTracker == null) {
      throw new InternalException("Analytics Tracker is not enabled");
    }
    return this.analyticsTracker;
  }

  public TowerSmtpClientService getSmtpClient() {
    if (this.smtpClient == null) {
      throw new InternalException("Smtp Client is not enabled");
    }
    return this.smtpClient;
  }

  public TowerDnsClient getDnsClient() {
    if (this.dnsClient == null) {
      throw new InternalException("Dns Client is not enabled");
    }
    return this.dnsClient;
  }

  public MapDb getMapDb() {
    if (this.mapDb == null) {
      throw new InternalException("Map Db is not enabled");
    }
    return this.mapDb;

  }


  public void closeServices() throws Exception {
    MainLauncher.prometheus.close();
    for (TowerService closable : this.services) {
      LOGGER.info("Closing " + closable.getClass().getSimpleName());
      closable.close();
    }
  }

  public TowerFutures getFutureSchedulers() {
    return new TowerFutures(this);
  }

  public void registerService(TowerService service) {
    this.services.add(service);
  }


  /**
   * @return a health check object where services/object can register health checks
   */
  public ServerHealth getServerHealthCheck() {

    return this.builder.serverHealth;

  }

  public List<TowerService> getServices() {
    return this.services;
  }

  public WriteThroughCollection getWriteThroughCollection() {
    if (this.writeThroughCollection == null) {
      throw new InternalException("Write Through Collection is not enabled");
    }
    return this.writeThroughCollection;
  }


  public static class builder {
    private final String name;
    private final Vertx vertx;
    private final ConfigAccessor configAccessor;
    private int listeningPort;
    private int publicPort;
    private String listeningHost;
    private Boolean ssl = false;
    private String postgresPoolName;
    private boolean enableIpGeoLocation = false;
    private boolean addJwt = false;
    private boolean addApiKeyAuth = false;
    private boolean enableHashId = false;
    private boolean enableJacksonTime = true;
    private boolean enableJsonToken = false;
    private boolean enableAnalytics = false;

    private String smtpClientUserAgentName = null;
    private boolean enableMapdb = false;
    private boolean enableDnsClient = false;
    private ServerHealth serverHealth;
    private boolean enableWriteThroughCollection = false;

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

    public Server build() throws ConfigIllegalException {
      Server server = new Server(this);

      /**
       * Data Type, used overal and also in the below handler
       * Should be first
       */
      server.jacksonMapperManager = JacksonMapperManager.create(server);
      if (this.enableJacksonTime) {
        server.jacksonMapperManager.enableTimeModuleForVertx();
      } else {
        LOGGER.info("Jackson time not enabled for vertx");
      }

      /**
       * At the beginning after data type (jackson) so that
       * other service can register health checks
       */
      this.serverHealth = new ServerHealth(server);

      if (this.postgresPoolName != null) {
        LOGGER.info("Start creation of JDBC Pool (" + this.postgresPoolName + ")");
        server.pgDatabaseConnectionPool = JdbcPostgres.create(server, this.postgresPoolName);
      }
      if (this.enableIpGeoLocation) {
        try {
          server.ipGeolocation = IpGeolocation.create(server.pgDatabaseConnectionPool);
        } catch (DbMigrationException e) {
          throw new ConfigIllegalException("Ip geolocation bad schema migration", e);
        }
      } else {
        LOGGER.info("IP Geo-location not enabled");
      }
      if (this.addJwt) {
        try {
          server.jwtAuthManager = JwtAuthManager.create(server);
        } catch (NoSecretException e) {
          throw new ConfigIllegalException("Unable to init JWT", e);
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


      if (this.enableJsonToken) {
        server.jsonToken = new JsonToken.config(configAccessor).create();
      }

      /**
       * Before Failure Handler as this service uses SMTP
       */
      if (this.smtpClientUserAgentName != null) {
        LOGGER.info("Smtp Client Enabled: Start Instantiation of Email Engine");
        BMailSmtpConnectionParameters mailSmtpParameterFromConfig = ConfigMailSmtpParameters.createFromConfigAccessor(configAccessor);
        server.smtpClient = TowerSmtpClientService
          .config(this.smtpClientUserAgentName, server, mailSmtpParameterFromConfig)
          .create();
        Log4jConfigure.configureOnVertxInit(mailSmtpParameterFromConfig);
      } else {
        LOGGER.info("Smtp Client disabled");
      }

      /**
       * Failure Handler (Always on)
       */
      server.failureHandler = new TowerFailureHandler(server);
      vertx.exceptionHandler(server.failureHandler);
      LOGGER.info("Vertx Failure Handler started");

      /**
       * Before Analytics service as this service uses MapDb
       */
      if (this.enableMapdb) {
        LOGGER.info("MapDb enabled");
        server.mapDb = new MapDb(server);
        server.services.add(server.mapDb);
      } else {
        LOGGER.info("MapDb disabled");
      }

      if (this.enableWriteThroughCollection) {
        LOGGER.info("Write Through Collection enabled");
        server.writeThroughCollection = new WriteThroughCollection(server);
      } else {
        LOGGER.info("Write Through Collection disabled");
      }

      if (this.enableAnalytics) {
        LOGGER.info("Analytics tracker enabled");
        server.analyticsTracker = AnalyticsTracker.createFromServer(server);
      } else {
        LOGGER.info("Analytics tracker disabled");
      }

      if (this.enableDnsClient) {
        server.dnsClient = new TowerDnsClient(server);
      }

      return server;
    }

    /**
     * Enable a database where all prefix starts with jdbc
     */
    public builder enablePostgresDatabase() {
      this.enablePostgresDatabase("pg");
      return this;
    }

    /**
     * @param prefixName - the name is used in the configuration as prefix
     */
    public builder enablePostgresDatabase(String prefixName) {
      this.postgresPoolName = prefixName;
      return this;
    }


    public Server.builder addIpGeolocation() {
      if (this.postgresPoolName == null) {
        throw new InternalException("To enable Ip Geolocation, the jdbc pool service should be enabled first");
      }
      this.enableIpGeoLocation = true;
      return this;
    }

    /**
     * Add a JWT manager to create JWT token and authenticate
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
     * Add a write-through to database runtime collection
     * capabilities
     * If enabled, this service will create a schema called cs_collection
     */
    public Server.builder enableWriteThroughCollection() {
      this.enableWriteThroughCollection = true;
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

    /**
     * Enable the Json Token utility
     * and allow to pass encrypted data
     * that allow to authenticate the data when
     * received back.
     */
    public Server.builder enableJsonToken() {
      this.enableJsonToken = true;
      return this;
    }

    public Server.builder enableDnsClient() {
      this.enableDnsClient = true;
      return this;
    }


    public Server.builder enableTrackerAnalytics() {
      this.enableMapdb = true;
      this.enableAnalytics = true;
      return this;
    }

    public Server.builder enableMapDb() {
      this.enableMapdb = true;
      return this;
    }

    public Server.builder enableSmtpClient() {
      enableSmtpClient("Eraldy.com");
      return this;
    }

    public Server.builder enableSmtpClient(String userAgentName) {
      this.smtpClientUserAgentName = userAgentName;
      return this;
    }
  }
}
