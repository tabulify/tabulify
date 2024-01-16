package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.ListBody;
import net.bytle.tower.eraldy.model.openapi.ListItem;

import java.util.List;

public interface AppApi  {

  /**
     * Create a list where users can register
    */
    Future<ApiResponse<ListItem>> appAppListPost(RoutingContext routingContext, String appIdentifier, ListBody listBody, String realmIdentifier);

  /**
   * Retrieve an app by identifier (ie guid or handle)
    */
    Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appIdentifier, String realmIdentifier);

  /**
     * Create or modify an app (ie design, ...)
    */
    Future<ApiResponse<App>> appPost(RoutingContext routingContext, AppPostBody appPostBody);

  /**
     * List apps for a realm
    */
    Future<ApiResponse<List<App>>> appsGet(RoutingContext routingContext, String realmIdentifier);
}
