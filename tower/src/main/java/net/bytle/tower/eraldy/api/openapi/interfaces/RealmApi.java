package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;

import java.util.List;

public interface RealmApi  {

    /**
     * Return the asked realm
    */
    Future<ApiResponse<Realm>> realmGet(RoutingContext routingContext, String realmIdentifier);

    /**
     * Create a realm
    */
    Future<ApiResponse<Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPostBody);

    /**
     * Get the lists for an realm
    */
    Future<ApiResponse<List<ListObject>>> realmRealmIdentifierListsGet(RoutingContext routingContext, String realmIdentifier);

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
     * Return the list of realms owned by the authenticated user. The authenticated user needs to be an organizational user.
    */
    Future<ApiResponse<List<Realm>>> realmsOwnedByMeGet(RoutingContext routingContext);
}
