package net.bytle.tower.eraldy.api.openapi.interfaces;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

public interface HealthApi  {
    Future<ApiResponse<Void>> pingGet(RoutingContext routingContext);
}
