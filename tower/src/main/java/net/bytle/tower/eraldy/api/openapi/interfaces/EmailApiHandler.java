package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailApiHandler {

private static final Logger logger = LoggerFactory.getLogger(EmailApiHandler.class);

private final EmailApi api;

public EmailApiHandler(EmailApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("emailAddressAddressValidateGet").handler(this::emailAddressAddressValidateGet);
}

    private void emailAddressAddressValidateGet(RoutingContext routingContext) {
    logger.info("emailAddressAddressValidateGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String address = requestParameters.pathParameter("address") != null ? requestParameters.pathParameter("address").getString() : null;

      logger.debug("Parameter address is {}", address);

    // Based on Route#respond
    api.emailAddressAddressValidateGet(routingContext, address)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
