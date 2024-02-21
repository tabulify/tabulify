package net.bytle.ip.api;

import io.vertx.core.Future;
import net.bytle.exception.NotFoundException;
import net.bytle.ip.IpApp;
import net.bytle.vertx.HttpHeaders;
import net.bytle.vertx.IpInfo;
import net.bytle.vertx.RoutingContextWrapper;

/**
 * See also
 * <a href="https://ipapi.co/json">...</a>
 */
public class IpApiImpl implements IpApi {


  private final IpApp ipApp;

  public IpApiImpl(IpApp ipApp) {
    this.ipApp = ipApp;
  }


  @Override
  public Future<IpInfo> ipGet(RoutingContextWrapper routingContext) {
    String ip;
    try {
      ip = routingContext.getRealRemoteClientIp();
    } catch (NotFoundException e) {
      return Future.failedFuture(e);
    }
    return this.ipIpGet(routingContext, ip);
  }

  @Override
  public Future<IpInfo> ipIpGet(RoutingContextWrapper routingContext, String ip) {

    routingContext.response().putHeader("Content-Type", "application/json");
    routingContext.response().putHeader(HttpHeaders.CACHE_CONTROL, HttpHeaders.CACHE_CONTROL_NO_STORE);

    return this.ipApp.getHttpServer().getServer().getIpGeolocation().ipGet(ip);

  }

}
