package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(UserPublicapiHandler.class);

private final UserPublicapi api;

public UserPublicapiHandler(UserPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("userGet").handler(this::userGet);
}

    private void userGet(RoutingContext routingContext) {
    logger.info("userGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.userGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
