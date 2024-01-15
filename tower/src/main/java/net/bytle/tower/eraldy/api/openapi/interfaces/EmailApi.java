package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.json.JsonObject;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface EmailApi  {

  /**
     * Validate an email address
    */
    Future<ApiResponse<JsonObject>> emailAddressAddressValidateGet(RoutingContext routingContext, String address, Boolean failEarly);
}
