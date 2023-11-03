package net.bytle.tower.eraldy.api.implementer;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.interfaces.HealthApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.vertx.HealthChecksEventBus;
import net.bytle.vertx.TowerApp;

/**
 * Implementation Class
 */

public class HealthApiImpl implements HealthApi {

  public static String PING_PATH = "/ping";

  public HealthApiImpl(@SuppressWarnings("unused") TowerApp towerApp) {

  }

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
