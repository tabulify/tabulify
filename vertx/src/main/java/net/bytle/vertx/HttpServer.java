package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;

/**
 * This class represents an HTTP server:
 * with standard handlers
 */
public class HttpServer {

  private final HttpServer.builder builder;
  private Router router;

  private APIKeyHandler apiKeyAuthenticator;
  private JWTAuthHandler bearerAuthenticationHandler;
  private BasicAuthHandler basicAuthenticator;
  private APIKeyHandler cookieAuthenticator;


  public HttpServer(builder builder) {
    this.builder = builder;
  }


  public static builder createFromServer(Server server) {

    return new HttpServer.builder(server);

  }


  public Router getRouter() {
    return this.router;
  }


  /**
   * This port is the port that is all external communication
   * (in email, in oauth callback, ...)
   */

  public io.vertx.core.http.HttpServer buildHttpServer() {
    HttpServerOptions options = new HttpServerOptions()
      .setLogActivity(false)
      .setHost(this.builder.server.getListeningHost())
      .setPort(this.builder.server.getListeningPort());
    /**
     * https://vertx.io/docs/apidocs/io/vertx/core/net/PemKeyCertOptions.html
     */
    if (this.getServer().getSsl()) {
      options
        .setPemKeyCertOptions(
          new PemKeyCertOptions().addKeyPath(Server.DEV_KEY_PEM).addCertPath(Server.DEV_CERT_PEM)
        )
        .setSsl(true);
    }
    return this.builder.server.getVertx().createHttpServer(options);
  }

  public int getPublicPort() {
    return this.builder.server.getPublicPort();
  }


  /**
   * @return the scheme always secure
   */
  public String getHttpScheme() {
    if (isHttpsEnabled()) return "https";
    return "http";
  }

  /**
   * @return if https is enabled on the system
   * See the https.md documentation for more info.
   */
  public boolean isHttpsEnabled() {

    return this.builder.server.getSsl();
  }

  public Server getServer() {
    return this.builder.server;
  }


  /**
   * @return the JWT bearer authentication handler
   * An utility class, JWT should be enabled on the server
   */
  @SuppressWarnings("unused")
  public JWTAuthHandler getBearerAuthenticationHandler() {
    if (bearerAuthenticationHandler == null) {
      bearerAuthenticationHandler = JWTAuthHandler.create(this.getServer().getJwtAuth().getProvider());
    }
    return this.bearerAuthenticationHandler;
  }

  /**
   * @return the API key handler
   * An utility class, API key should be enabled on the server
   */
  public APIKeyHandler getApiKeyAuthHandler() {
    if (this.apiKeyAuthenticator == null) {
      throw new InternalException("Api Key Handler was not initialized");
    }
    return this.apiKeyAuthenticator;
  }

  /**
   * @return a basic auth handler
   */
  @SuppressWarnings("unused")
  public BasicAuthHandler getBasicAuthHandler() {
    if (this.basicAuthenticator == null) {
      this.basicAuthenticator = BasicAuthHandler.create(this.getServer().getApiKeyAuth());
    }
    return this.basicAuthenticator;
  }

  @SuppressWarnings("unused")
  public APIKeyHandler getCookieAuthHandler() throws IllegalConfiguration {
    if (this.cookieAuthenticator == null) {
      throw new IllegalConfiguration("The cookie auth handler was not enabled");
    }
    return this.cookieAuthenticator;
  }


  public static class builder {

    private boolean addBodyHandler = true;
    private boolean addWebLog = true;
    private boolean isBehindProxy = true;
    private boolean enableFailureHandler = true;
    private boolean enableMetrics = true;
    private boolean fakeErrorHandler = false;
    private boolean healthCheck = false;
    final Server server;
    private String sessionCookieAuthName;


    public builder(Server server) {
      this.server = server;
    }


    /**
     * Logging Web Request
     */
    public HttpServer.builder addWebLog() {
      this.addWebLog = true;
      return this;
    }

    /**
     * Forward proxy is disabled by default
     */
    public HttpServer.builder setBehindProxy() {
      this.isBehindProxy = true;
      return this;
    }

    public HttpServer.builder enableFailureHandler() {
      this.enableFailureHandler = true;
      return this;
    }

    public HttpServer.builder addMetrics() {
      this.enableMetrics = true;
      return this;
    }

    public HttpServer.builder addFakeErrorHandler() {
      this.fakeErrorHandler = true;
      return this;
    }

    public HttpServer.builder addHealthCheck() {
      this.healthCheck = true;
      return this;

    }

    /**
     * A handler which gathers the entire request body and sets it on the {@link RoutingContext}
     * You can't request the body from the request after-wards
     * You need to get if from the context object
     * <p>
     * BodyHandler is required to process POST requests for instance
     */
    public HttpServer.builder addBodyHandler() {
      this.addBodyHandler = true;
      return this;
    }

    private Router buildRouter() throws IllegalConfiguration {
      Vertx vertx = this.server.getVertx();
      Router router = Router.router(vertx);
      if (this.addBodyHandler) {
        router.route().handler(BodyHandler.create());
      }
      if (this.addWebLog) {
        router.route().handler(new WebLogger(LoggerFormat.DEFAULT));
      }
      if (this.isBehindProxy) {
        HttpForwardProxy.addAllowForwardProxy(router);
      }
      if (this.enableFailureHandler) {

        /**
         * Failure Handler / Route match failures
         * https://vertx.io/docs/vertx-web/java/#_route_match_failures
         */
        VertxRoutingFailureHandler errorHandlerXXX = VertxRoutingFailureHandler.createOrGet(vertx, this.server.getConfigAccessor());
        router.errorHandler(HttpStatusEnum.INTERNAL_ERROR_500.getStatusCode(), errorHandlerXXX);

        /**
         * Handle the failures. ie
         * ```
         * ctx.fail(400, error)
         * ```
         */
        router.route().failureHandler(errorHandlerXXX);

      }
      if (this.enableMetrics) {
        VertxPrometheusMetrics.mountOnRouter(router, VertxPrometheusMetrics.DEFAULT_METRICS_PATH);
      }
      if (this.fakeErrorHandler) {
        /**
         * Produce an error, not only for dev, also for production to live test (mail, ...)
         */
        router.get(ErrorFakeHandler.URI_PATH).handler(new ErrorFakeHandler());
      }
      if (this.healthCheck) {
        HealthChecksRouter.addHealtChecksToRouter(router, this.server.getVertx(), this);
        HealthChecksEventBus.registerHandlerToEventBus(vertx);
      }
      return router;
    }

    public HttpServer build() throws IllegalConfiguration {
      HttpServer httpserver = new HttpServer(this);
      httpserver.router = this.buildRouter();
      if (this.sessionCookieAuthName != null) {
        httpserver.cookieAuthenticator = APIKeyHandler
          .create(this.server.getApiKeyAuth())
          .cookie(this.sessionCookieAuthName);
      }
      httpserver.apiKeyAuthenticator = APIKeyHandler.create(this.server.getApiKeyAuth());
      return httpserver;
    }


    /**
     * Enable cookie authentication
     *
     * @param cookieName - the name of the cookie where the token/session id is stored
     */
    public HttpServer.builder enableSessionCookieAuth(String cookieName) {
      this.sessionCookieAuthName = cookieName;
      return this;
    }

  }
}
