package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;

import java.util.List;

public interface UserApi  {

    /**
     * Get a user by guid or email
    */
    Future<ApiResponse<User>> userGet(RoutingContext routingContext, String userIdentifier, String realmIdentifier);

    /**
     * Get a user by guid  If you want to use the email has identifier, you should use the `userGet` operation passing the email as query parameter
    */
    Future<ApiResponse<User>> userGuidGet(RoutingContext routingContext, String guid);

    /**
     * Get the authenticated user
    */
    Future<ApiResponse<User>> userMeGet(RoutingContext routingContext);

    /**
     * Create or modify a user (a guid or a email should be given)
    */
    Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody);

    /**
     * List users
    */
    Future<ApiResponse<List<User>>> usersGet(RoutingContext routingContext, String realmIdentifier);
}
