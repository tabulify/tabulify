package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.CspObject;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface CspApi  {

  /**
     * Report a CSP Violation
    */
    Future<ApiResponse<Void>> cspReportPost(RoutingContext routingContext, CspObject cspObject);
}
