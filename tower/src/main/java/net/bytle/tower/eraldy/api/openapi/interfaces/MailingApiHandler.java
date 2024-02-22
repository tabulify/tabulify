package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailingApiHandler {

private static final Logger logger = LoggerFactory.getLogger(MailingApiHandler.class);

private final MailingApi api;

public MailingApiHandler(MailingApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("mailingIdentifierGet").handler(this::mailingIdentifierGet);
}

    private void mailingIdentifierGet(RoutingContext routingContext) {
    logger.info("mailingIdentifierGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String mailingIdentifier = requestParameters.pathParameter("mailingIdentifier") != null ? requestParameters.pathParameter("mailingIdentifier").getString() : null;

      logger.debug("Parameter mailingIdentifier is {}", mailingIdentifier);

    // Based on Route#respond
    api.mailingIdentifierGet(routingContext, mailingIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
