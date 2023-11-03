package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;

import java.util.List;

public interface UserApi  {
    Future<ApiResponse<OrganizationUser>> userAuthGet(RoutingContext routingContext);
    Future<ApiResponse<User>> userGet(RoutingContext routingContext, String userGuid, String userEmail, String realmHandle, String realmGuid);
    Future<ApiResponse<User>> userGuidGet(RoutingContext routingContext, String guid);
    Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody);
    Future<ApiResponse<List<User>>> usersGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
