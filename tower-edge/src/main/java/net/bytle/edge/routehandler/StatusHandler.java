package net.bytle.edge.routehandler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class StatusHandler implements Handler<RoutingContext> {
  public static Handler<RoutingContext> create() {
    return new StatusHandler();
  }

  @Override
  public void handle(RoutingContext event) {

    event
      .json(JsonObject.of("status", "ok"));

  }
}
