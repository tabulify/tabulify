package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface OrganizationApi  {

  /**
     * Get the users of the organization
    */
    Future<ApiResponse<List<User>>> orgaOrgaUsersGet(RoutingContext routingContext, String orgaIdentifier);

  /**
     * Get the authenticated user and its organization
    */
    Future<ApiResponse<OrganizationUser>> organizationUserMeGet(RoutingContext routingContext);
}
