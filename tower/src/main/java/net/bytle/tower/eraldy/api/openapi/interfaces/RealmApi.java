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
    Future<ApiResponse<RealmAnalytics>> realmGet(RoutingContext routingContext, String realmIdentifier);

    /**
     * Create a realm
    */
    Future<ApiResponse<Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPostBody);

    /**
     * Return the list of recent new users for the realm
    */
    Future<ApiResponse<List<User>>> realmUsersNewGet(RoutingContext routingContext, String realmIdentifier);

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
