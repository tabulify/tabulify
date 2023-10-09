package net.bytle.tower.eraldy.app.comboprivateapi.implementer;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces.HealthComboprivateapi;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;

/**
 * Implementation Class
 */

public class HealthComboprivateapiImpl implements HealthComboprivateapi {

  public static String PING_PATH = "/ping";

  @Override
  public Future<ApiResponse<Void>> pingGet(RoutingContext routingContext) {
    ApiResponse<Void> apiResponse = new ApiResponse<>();
    return Future.succeededFuture(apiResponse);
  }

}
