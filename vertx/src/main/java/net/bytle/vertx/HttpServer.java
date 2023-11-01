package net.bytle.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.java.JavaEnvs;

/**
 * This class represents an HTTP server:
 * with standard handlers
 */
public class HttpServer {

  private final HttpServer.builder builder;
  private Router router;


  public HttpServer(builder builder) {
    this.builder = builder;
  }


  public static builder create(String prefix, AbstractVerticle verticle, ConfigAccessor configAccessor, int portDefault) {

    return new HttpServer.builder(prefix, verticle, configAccessor, portDefault);

  }


  public Router getRouter() {
    return this.router;
  }


  /**
   * This port is the port that is all external communication
   * (in email, in oauth callback, ...)
   */

  public io.vertx.core.http.HttpServer getServer() {
    HttpServerOptions options = new HttpServerOptions()
      .setLogActivity(false)
      .setHost(this.builder.serverProperties.getListeningHost())
      .setPort(this.builder.serverProperties.getListeningPort());
    /**
     * https://vertx.io/docs/apidocs/io/vertx/core/net/PemKeyCertOptions.html
     */
    if (JavaEnvs.IS_DEV) {
      options
        .setPemKeyCertOptions(
          new PemKeyCertOptions().addKeyPath(ServerProperties.DEV_KEY_PEM).addCertPath(ServerProperties.DEV_CERT_PEM)
        )
        .setSsl(true);
    }
    return this.builder.verticle.getVertx().createHttpServer(options);
  }

  public int getPublicPort() {
    return this.builder.serverProperties.getPublicPort();
  }

  public Vertx getVertx() {
    return this.builder.verticle.getVertx();
  }

  public ConfigAccessor getConfigAccessor() {
    return this.builder.configAccessor;
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
    /**
     * Note that Chrome does not allow to set a third-party cookie (ie same site: None)
     * if the connection is not secure.
     * It must be true then everywhere.
     * For non-app, https comes from the proxy.
     */
    return true;
  }

  public ServerProperties getServerProperties() {
    return this.builder.serverProperties;
  }


  public static class builder {
    private final AbstractVerticle verticle;
    private final ConfigAccessor configAccessor;
    private final int portDefault;
    private final String prefix;

    private boolean addBodyHandler = true;
    private boolean addWebLog = true;
    private boolean isBehindProxy = true;
    private boolean enableFailureHandler = true;
    private boolean enableMetrics = true;
    private boolean fakeErrorHandler = false;
    private boolean healthCheck = false;
    ServerProperties serverProperties;

    public builder(String prefix, AbstractVerticle verticle, ConfigAccessor configAccessor, int portDefault) {
      this.prefix = prefix;
      this.verticle = verticle;
      this.configAccessor = configAccessor;
      this.portDefault = portDefault;
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
      Vertx vertx = this.verticle.getVertx();
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
        VertxRoutingFailureHandler errorHandlerXXX = VertxRoutingFailureHandler.createOrGet(vertx, this.verticle.config());
        router.errorHandler(HttpStatus.INTERNAL_ERROR, errorHandlerXXX);

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
        HealthChecksRouter.addHealtChecksToRouter(router, this.verticle, this);
        HealthChecksEventBus.registerHandlerToEventBus(vertx);
      }
      return router;
    }

    public HttpServer build() throws IllegalConfiguration {
      HttpServer httpBuilder = new HttpServer(this);
      // serverProperties is used to build the router and should be then local
      serverProperties = ServerProperties
        .create(this.prefix)
        .fromConfigAccessor(this.configAccessor, this.portDefault)
        .build();
      httpBuilder.router = this.buildRouter();
      return httpBuilder;
    }

  }
}
