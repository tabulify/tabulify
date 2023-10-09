package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;

import java.util.Map;

public interface AnalyticsPublicapi  {
    Future<ApiResponse<Void>> analyticsEventPost(RoutingContext routingContext, Map<String, Object> requestBody);
}
