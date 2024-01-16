package net.bytle.tower.eraldy.api.openapi.interfaces;


import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvApiHandler {

private static final Logger logger = LoggerFactory.getLogger(EnvApiHandler.class);

private final EnvApi api;

public EnvApiHandler(EnvApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("envGet").handler(this::envGet);
}

    private void envGet(RoutingContext routingContext) {
    logger.info("envGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.envGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
