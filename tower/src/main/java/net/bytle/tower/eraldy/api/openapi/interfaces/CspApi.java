package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.CspObject;

public interface CspApi  {

    /**
     * Report a CSP Violation
    */
    Future<ApiResponse<Void>> cspReportPost(RoutingContext routingContext, CspObject cspObject);
}
