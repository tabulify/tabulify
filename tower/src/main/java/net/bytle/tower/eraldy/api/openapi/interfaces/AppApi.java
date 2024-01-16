package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.ListBody;
import net.bytle.tower.eraldy.model.openapi.ListItem;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

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
