package net.bytle.ip.api;

import io.vertx.core.Future;
import net.bytle.vertx.IpInfo;
import net.bytle.vertx.RoutingContextWrapper;


public interface IpApi {
    Future<IpInfo> ipGet(RoutingContextWrapper routingContext);
    Future<IpInfo> ipIpGet(RoutingContextWrapper routingContext, String ip);
}
