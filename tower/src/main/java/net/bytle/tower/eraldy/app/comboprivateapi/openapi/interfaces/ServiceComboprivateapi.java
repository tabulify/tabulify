package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Service;
import net.bytle.tower.eraldy.model.openapi.ServiceSmtpPostBody;

import java.util.List;

public interface ServiceComboprivateapi  {
    Future<ApiResponse<Service>> serviceGet(RoutingContext routingContext, String serviceGuid, String serviceUri, String realmHandle, String realmGuid);
    Future<ApiResponse<Service>> serviceSmtpPost(RoutingContext routingContext, ServiceSmtpPostBody serviceSmtpPostBody);
    Future<ApiResponse<List<Service>>> servicesGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
