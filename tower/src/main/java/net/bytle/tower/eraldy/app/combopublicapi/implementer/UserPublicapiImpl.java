package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.UserPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.tower.util.HttpStatus;

public class UserPublicapiImpl implements UserPublicapi {

    @Override
    public Future<ApiResponse<User>> userGet(RoutingContext routingContext) {

        try {
            return AuthInternalAuthenticator.getAuthUserFromContext(routingContext)
                    .onFailure(routingContext::fail)
                    .compose(comboUser -> {
                        comboUser.setOrganization(null);
                        User publicUser = UserProvider.createFrom(routingContext.vertx())
                                .toPublicCloneWithRealm(comboUser);
                        return Future.succeededFuture((new ApiResponse<>(publicUser)));
                    });
        } catch (NotFoundException e) {
            return Future.succeededFuture((new ApiResponse<>(HttpStatus.NOT_AUTHORIZED)));
        }

    }


}
