package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Service;
import net.bytle.tower.eraldy.model.openapi.ServiceSmtpPostBody;

import java.util.List;

public interface ServiceApi  {

    /**
     * Retrieve a service by guid or uri
    */
    Future<ApiResponse<Service>> serviceGet(RoutingContext routingContext, String serviceGuid, String serviceUri, String realmHandle, String realmGuid);

    /**
     * Create a SMTP mail service for a app or a user
    */
    Future<ApiResponse<Service>> serviceSmtpPost(RoutingContext routingContext, ServiceSmtpPostBody serviceSmtpPostBody);

    /**
     * Get all services
    */
    Future<ApiResponse<List<Service>>> servicesGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
