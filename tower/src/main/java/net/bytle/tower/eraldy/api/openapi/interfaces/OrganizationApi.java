package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;

public interface OrganizationApi  {

    /**
     * Get the authenticated user and its organization
    */
    Future<ApiResponse<OrganizationUser>> organizationUserMeGet(RoutingContext routingContext);
}
