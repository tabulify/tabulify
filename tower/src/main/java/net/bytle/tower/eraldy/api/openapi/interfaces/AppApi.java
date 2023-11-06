package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;

import java.util.List;

public interface AppApi  {

    /**
     * Retrieve an app by: * id with the appGuid * or by name with the appUri and realm (Handle or Guid)
    */
    Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appGuid, String appUri, String realmHandle, String realmGuid);

    /**
     * Create or modify an app (ie design, ...)
    */
    Future<ApiResponse<App>> appPost(RoutingContext routingContext, AppPostBody appPostBody);

    /**
     * List apps for a realm
    */
    Future<ApiResponse<List<App>>> appsGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
