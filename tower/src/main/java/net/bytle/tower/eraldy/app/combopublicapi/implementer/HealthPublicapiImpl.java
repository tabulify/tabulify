package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.HealthPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.util.HealthChecksEventBus;

public class HealthPublicapiImpl implements HealthPublicapi {
  @Override
  public Future<ApiResponse<Void>> pingGet(RoutingContext routingContext) {

    return routingContext.vertx().eventBus()
      .request(HealthChecksEventBus.HEALTH_EVENT_BUS_ADDRESS, "")
      .compose(message -> {
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        return Future.succeededFuture(apiResponse);
      });


  }
}
