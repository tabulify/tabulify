package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;

import java.util.List;

public interface AppApi  {
    Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appGuid, String appUri, String realmHandle, String realmGuid);
    Future<ApiResponse<App>> appPost(RoutingContext routingContext, AppPostBody appPostBody);
    Future<ApiResponse<List<App>>> appsGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
