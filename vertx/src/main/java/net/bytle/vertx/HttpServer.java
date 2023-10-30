package net.bytle.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import net.bytle.exception.IllegalConfiguration;

/**
 * A builder to create quickly a HTTP server
 * with standard handlers
 */
public class HttpServer {

  /**
   * Listen from all hostname
   * On ipv4 and Ipv6.
   * The wildcard implementation depends on the language
   * and in Java it works for the 2 Ip formats.
   */
  public static final String WILDCARD_IPV4_ADDRESS = "0.0.0.0";
  @SuppressWarnings("unused")
  public static final String WILDCARD_IPV6_ADDRESS = "[::]";
  private final HttpServer.builder builder;
  private Router router;


  public HttpServer(builder builder) {
    this.builder = builder;
  }


  public static builder create(AbstractVerticle verticle, ConfigAccessor configAccessor, int portDefault) {

    return new HttpServer.builder(verticle, configAccessor, portDefault);

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
      .setHost(this.builder.getListeningHost())
      .setPort(this.builder.getListeningPort());
    HttpsCertificateUtil.createOrGet()
      .enableServerHttps(options);
    return this.builder.verticle.getVertx().createHttpServer(options);
  }

  public int getPublicPort() {
    return this.builder.getPublicPort();
  }

  public Vertx getVertx() {
    return this.builder.verticle.getVertx();
  }

  public ConfigAccessor getConfigAccessor() {
    return this.builder.configAccessor;
  }

  public static class builder {
    private final AbstractVerticle verticle;
    private final ConfigAccessor configAccessor;
    private final int portDefault;

    private boolean addBodyHandler = true;
    private boolean addWebLog = true;
    private boolean isBehindProxy = true;
    private boolean enableFailureHandler = true;
    private boolean enableMetrics = true;
    private boolean fakeErrorHandler = false;
    private boolean healthCheck = false;

    public builder(AbstractVerticle verticle, ConfigAccessor configAccessor, int portDefault) {
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


    public String getListeningHost() {

      return configAccessor.getString(ServerProperties.HOST.toString(), WILDCARD_IPV4_ADDRESS);

    }

    public Integer getListeningPort() {
      return configAccessor.getInteger(ServerProperties.LISTENING_PORT.toString(), portDefault);
    }

    public Integer getPublicPort(){
      return configAccessor.getInteger(ServerProperties.PUBLIC_PORT.toString(), 80);
    }

    /**
     * A handler which gathers the entire request body and sets it on the {@link RoutingContext}
     * You can't request the body from the request afterwards
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
      httpBuilder.router = this.buildRouter();
      return httpBuilder;
    }

  }
}
