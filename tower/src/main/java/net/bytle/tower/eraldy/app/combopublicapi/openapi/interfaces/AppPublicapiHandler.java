package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(AppPublicapiHandler.class);

private final AppPublicapi api;

public AppPublicapiHandler(AppPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("appGet").handler(this::appGet);
}

    private void appGet(RoutingContext routingContext) {
    logger.info("appGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.appGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
