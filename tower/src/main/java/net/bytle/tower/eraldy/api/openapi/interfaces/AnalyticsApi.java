package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.AnalyticsEvent;

import java.util.List;
import java.util.Map;

public interface AnalyticsApi  {

    /**
     * Return a list of events
    */
    Future<ApiResponse<List<AnalyticsEvent>>> analyticsEventNameGet(RoutingContext routingContext, String eventName, String realmIdentifier, String appIdentifier);

    /**
     * Report a analytics event
    */
    Future<ApiResponse<Void>> analyticsEventPost(RoutingContext routingContext, Map<String, Object> requestBody);
}
