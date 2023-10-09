package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.ListPostBody;
import net.bytle.tower.eraldy.model.openapi.ListSummary;
import net.bytle.tower.eraldy.model.openapi.RegistrationList;

import java.util.List;

public interface ListComboprivateapi  {
    Future<ApiResponse<RegistrationList>> listGet(RoutingContext routingContext, String listGuid, String listHandle, String realmHandle);
    Future<ApiResponse<RegistrationList>> listPost(RoutingContext routingContext, ListPostBody listPostBody);
    Future<ApiResponse<List<RegistrationList>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmGuid, String realmHandle);
    Future<ApiResponse<List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
