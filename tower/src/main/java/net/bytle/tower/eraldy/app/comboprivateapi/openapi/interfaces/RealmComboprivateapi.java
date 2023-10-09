package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.RealmAnalytics;
import net.bytle.tower.eraldy.model.openapi.RealmPostBody;
import net.bytle.tower.eraldy.model.openapi.User;

import java.util.List;

public interface RealmComboprivateapi  {
    Future<ApiResponse<RealmAnalytics>> realmGet(RoutingContext routingContext, String realmGuid, String realmHandle);
    Future<ApiResponse<Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPostBody);
    Future<ApiResponse<List<User>>> realmUsersNewGet(RoutingContext routingContext, String realmGuid);
    Future<ApiResponse<List<RealmAnalytics>>> realmsGet(RoutingContext routingContext);
    Future<ApiResponse<List<Realm>>> realmsOwnedByGet(RoutingContext routingContext, String userGuid);
}
