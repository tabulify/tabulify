package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;

import java.util.Map;

public interface EnvComboprivateapi  {
    Future<ApiResponse<Map<String, Object>>> envGet(RoutingContext routingContext);
}
