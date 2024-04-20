package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.User;

import java.util.List;

public interface OrganizationApi  {

    /**
     * Get the users of the organization
    */
    Future<ApiResponse<List<User>>> orgaOrgaUsersGet(RoutingContext routingContext, String orgaIdentifier);

    /**
     * Get the authenticated user and its organization
    */
    Future<ApiResponse<OrgaUser>> organizationUserMeGet(RoutingContext routingContext);
}
