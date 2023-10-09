package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(IpPublicapiHandler.class);

private final IpPublicapi api;

public IpPublicapiHandler(IpPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("ipGet").handler(this::ipGet);
    builder.operation("ipIpGet").handler(this::ipIpGet);
}

    private void ipGet(RoutingContext routingContext) {
    logger.info("ipGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.ipGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void ipIpGet(RoutingContext routingContext) {
    logger.info("ipIpGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String ip = requestParameters.pathParameter("ip") != null ? requestParameters.pathParameter("ip").getString() : null;

      logger.debug("Parameter ip is {}", ip);

    // Based on Route#respond
    api.ipIpGet(routingContext, ip)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
