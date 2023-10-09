package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.IpInfo;

public interface IpPublicapi  {
    Future<ApiResponse<IpInfo>> ipGet(RoutingContext routingContext);
    Future<ApiResponse<IpInfo>> ipIpGet(RoutingContext routingContext, String ip);
}
