package net.bytle.tower.eraldy.api.openapi.interfaces;


import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailApiHandler {

private static final Logger logger = LoggerFactory.getLogger(MailApiHandler.class);

private final MailApi api;

public MailApiHandler(MailApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("mailEmailEmailValidationGet").handler(this::mailEmailEmailValidationGet);
}

    private void mailEmailEmailValidationGet(RoutingContext routingContext) {
    logger.info("mailEmailEmailValidationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String email = requestParameters.pathParameter("email") != null ? requestParameters.pathParameter("email").getString() : null;

      logger.debug("Parameter email is {}", email);

    // Based on Route#respond
    api.mailEmailEmailValidationGet(routingContext, email)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
