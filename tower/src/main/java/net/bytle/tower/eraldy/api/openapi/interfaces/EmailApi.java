package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

public interface EmailApi  {

    /**
     * Validate an email address
    */
    Future<ApiResponse<JsonObject>> emailAddressAddressValidateGet(RoutingContext routingContext, String address, Boolean failEarly);
}
