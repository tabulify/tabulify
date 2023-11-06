package net.bytle.tower.eraldy.api.openapi.interfaces;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import java.util.Map;

public interface AnalyticsApi  {

    /**
     * Report a analytics event
    */
    Future<ApiResponse<Void>> analyticsEventPost(RoutingContext routingContext, Map<String, Object> requestBody);
}
