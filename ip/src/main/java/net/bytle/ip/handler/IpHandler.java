package net.bytle.ip.handler;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.ip.api.IpApi;
import net.bytle.vertx.RoutingContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpHandler {

  private static final Logger logger = LoggerFactory.getLogger(IpHandler.class);

  private final IpApi api;

  public IpHandler(IpApi api) {
    this.api = api;
  }

  public void mount(RouterBuilder builder) {
    builder.operation("ipGet").handler(this::ipGet);
    builder.operation("ipIpGet").handler(this::ipIpGet);
  }

  private void ipGet(RoutingContext routingContext) {
    logger.info("ipGet()");
    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    api.ipGet(routingContextWrapper)
      .onSuccess(routingContextWrapper::respond)
      .onFailure(routingContext::fail);
  }

  private void ipIpGet(RoutingContext routingContext) {
    logger.info("ipIpGet()");

    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

    String ip = requestParameters.pathParameter("ip") != null ? requestParameters.pathParameter("ip").getString() : null;

    logger.debug("Parameter ip is {}", ip);

    // Based on Route#respond
    api.ipIpGet(routingContextWrapper, ip)
      .onSuccess(routingContextWrapper::respond)
      .onFailure(routingContext::fail);
  }

}
