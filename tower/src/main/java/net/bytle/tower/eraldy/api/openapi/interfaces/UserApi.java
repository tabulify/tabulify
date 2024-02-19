package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;

public interface UserApi  {

    /**
     * Get the authenticated user
    */
    Future<ApiResponse<User>> userMeGet(RoutingContext routingContext);

    /**
     * Create or modify a user (a guid or a email should be given)
    */
    Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody);

    /**
     * Get a user by identifier (guid or email)
    */
    Future<ApiResponse<User>> userUserIdentifierGet(RoutingContext routingContext, String userIdentifier, String realmIdentifier);
}
