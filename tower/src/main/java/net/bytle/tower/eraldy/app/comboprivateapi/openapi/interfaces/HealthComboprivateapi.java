package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;

public interface HealthComboprivateapi  {
    Future<ApiResponse<Void>> pingGet(RoutingContext routingContext);
}
