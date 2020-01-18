package net.bytle.api.http;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyticsHandler implements Handler<RoutingContext> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public static final String ANALYTICS_PATH = "/analytics";
  private final Vertx vertx;

  /**
   * No singleton because we may start several Http Server Verticle
   * for test purpose
   * Use the below constructor please
   *
   * @param vertx
   */
  public AnalyticsHandler(Vertx vertx) {
    this.vertx = vertx;
  }



  @Override
  public void handle(RoutingContext context) {

    context.request().bodyHandler(bodyHandler -> {
      final JsonObject body = bodyHandler.toJsonObject();
      logger.info(body.toString());
      context.response()
        .setStatusCode(200)
        .end();
    });


  }

}
