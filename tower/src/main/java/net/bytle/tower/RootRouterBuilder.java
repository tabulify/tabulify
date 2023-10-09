package net.bytle.tower;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import net.bytle.tower.util.WebLogger;

public class RootRouterBuilder {


  private final Router router;

  public RootRouterBuilder(AbstractVerticle verticle) {
    Vertx vertx = verticle.getVertx();
    this.router = Router.router(vertx);
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
    router.route().handler(new WebLogger(LoggerFormat.DEFAULT));
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

    router.route().handler(BodyHandler.create());
    return this;
  }


  public Router getRouter() {
    return router;
  }


}
