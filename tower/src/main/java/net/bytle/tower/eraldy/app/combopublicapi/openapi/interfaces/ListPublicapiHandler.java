package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(ListPublicapiHandler.class);

private final ListPublicapi api;

public ListPublicapiHandler(ListPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("listsGet").handler(this::listsGet);
}

    private void listsGet(RoutingContext routingContext) {
    logger.info("listsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.listsGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
