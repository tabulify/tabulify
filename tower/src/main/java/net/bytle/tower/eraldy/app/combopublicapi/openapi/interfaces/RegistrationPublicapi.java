package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.ListRegistrationPostBody;
import net.bytle.vertx.ExitStatusResponse;

public interface RegistrationPublicapi  {
    Future<ApiResponse<ExitStatusResponse>> listRegistrationPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody);
}
