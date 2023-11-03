package net.bytle.tower.eraldy.api.openapi.interfaces;


import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthApiHandler {

private static final Logger logger = LoggerFactory.getLogger(HealthApiHandler.class);

private final HealthApi api;

public HealthApiHandler(HealthApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("pingGet").handler(this::pingGet);
}

    private void pingGet(RoutingContext routingContext) {
    logger.info("pingGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.pingGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
