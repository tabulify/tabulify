package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;

public interface HealthPublicapi  {
    Future<ApiResponse<Void>> pingGet(RoutingContext routingContext);
}
