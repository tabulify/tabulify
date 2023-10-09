package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(RealmPublicapiHandler.class);

private final RealmPublicapi api;

public RealmPublicapiHandler(RealmPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("realmGet").handler(this::realmGet);
}

    private void realmGet(RoutingContext routingContext) {
    logger.info("realmGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.realmGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
