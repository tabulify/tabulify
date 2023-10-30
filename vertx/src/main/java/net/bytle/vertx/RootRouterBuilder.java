package net.bytle.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import net.bytle.exception.IllegalConfiguration;

public class RootRouterBuilder {


  private final AbstractVerticle verticle;
  private boolean addBodyHandler = true;
  private boolean addWebLog = true;
  private boolean isBehindProxy = true;
  private boolean enableFailureHandler = true;
  private boolean enableMetrics = true;

  public RootRouterBuilder(AbstractVerticle verticle) {
    this.verticle = verticle;
  }


  /**
   * Routing
   * a router should not be shared between verticles.
   */
  public static RootRouterBuilder create(AbstractVerticle verticle) {


    return new RootRouterBuilder(verticle);

  }


  /**
   * Logging Web Request
   */
  public RootRouterBuilder addWebLog() {
    this.addWebLog = true;
    return this;
  }


  /**
   * A handler which gathers the entire request body and sets it on the {@link RoutingContext}
   * You can't request the body from the request afterwards
   * You need to get if from the context object
   * <p>
   * BodyHandler is required to process POST requests for instance
   */
  public RootRouterBuilder addBodyHandler() {
    this.addBodyHandler = true;
    return this;
  }


  public Router getRouter() throws IllegalConfiguration {
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
    return router;

  }

  /**
   * Forward proxy is disabled by default
   */
  public RootRouterBuilder setBehindProxy() {
    this.isBehindProxy = true;
    return this;
  }

  public RootRouterBuilder enableFailureHandler() {
    this.enableFailureHandler = true;
    return this;
  }

  public RootRouterBuilder addMetrics() {
    this.enableMetrics = true;
    return this;
  }
}
