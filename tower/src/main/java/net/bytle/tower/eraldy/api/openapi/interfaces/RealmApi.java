package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.RealmAnalytics;
import net.bytle.tower.eraldy.model.openapi.RealmPostBody;
import net.bytle.tower.eraldy.model.openapi.RealmWithAppUris;
import net.bytle.tower.eraldy.model.openapi.User;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface RealmApi  {

  /**
     * Return the asked realm
    */
    Future<ApiResponse<RealmAnalytics>> realmGet(RoutingContext routingContext, String realmIdentifier);

  /**
     * Create a realm
    */
    Future<ApiResponse<Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPostBody);

  /**
     * Get users for the realm for pagination
    */
    Future<ApiResponse<List<User>>> realmRealmUsersGet(RoutingContext routingContext, String realmIdentifier, Long pageSize, Long pageId, String searchTerm);

  /**
     * Return the list of realms
    */
    Future<ApiResponse<List<RealmWithAppUris>>> realmsGet(RoutingContext routingContext);

  /**
     * Return the list of realms owned by the user
    */
    Future<ApiResponse<List<Realm>>> realmsOwnedByGet(RoutingContext routingContext, String userGuid);

  /**
   * Return the list of realms owned by the authenticated user. The authenticated user needs to be a organizational user.
    */
    Future<ApiResponse<List<RealmAnalytics>>> realmsOwnedByMeGet(RoutingContext routingContext);
}
