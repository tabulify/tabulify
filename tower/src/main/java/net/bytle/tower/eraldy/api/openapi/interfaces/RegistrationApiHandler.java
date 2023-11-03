package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrationApiHandler {

private static final Logger logger = LoggerFactory.getLogger(RegistrationApiHandler.class);

private final RegistrationApi api;

public RegistrationApiHandler(RegistrationApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("listRegistrationGet").handler(this::listRegistrationGet);
    builder.operation("listRegistrationLetterConfirmationGet").handler(this::listRegistrationLetterConfirmationGet);
    builder.operation("listRegistrationLetterValidationGet").handler(this::listRegistrationLetterValidationGet);
    builder.operation("listRegistrationValidationGet").handler(this::listRegistrationValidationGet);
    builder.operation("listRegistrationsGet").handler(this::listRegistrationsGet);
}

    private void listRegistrationGet(RoutingContext routingContext) {
    logger.info("listRegistrationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String guid = requestParameters.queryParameter("guid") != null ? requestParameters.queryParameter("guid").getString() : null;
        String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String subscriberEmail = requestParameters.queryParameter("subscriberEmail") != null ? requestParameters.queryParameter("subscriberEmail").getString() : null;

      logger.debug("Parameter guid is {}", guid);
      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter subscriberEmail is {}", subscriberEmail);

    // Based on Route#respond
    api.listRegistrationGet(routingContext, guid, listGuid, subscriberEmail)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationLetterConfirmationGet(RoutingContext routingContext) {
    logger.info("listRegistrationLetterConfirmationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String subscriberName = requestParameters.queryParameter("subscriberName") != null ? requestParameters.queryParameter("subscriberName").getString() : null;
        String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String listName = requestParameters.queryParameter("listName") != null ? requestParameters.queryParameter("listName").getString() : null;
        String ownerName = requestParameters.queryParameter("ownerName") != null ? requestParameters.queryParameter("ownerName").getString() : null;
        String ownerEmail = requestParameters.queryParameter("ownerEmail") != null ? requestParameters.queryParameter("ownerEmail").getString() : null;
        String ownerLogo = requestParameters.queryParameter("ownerLogo") != null ? requestParameters.queryParameter("ownerLogo").getString() : null;

      logger.debug("Parameter subscriberName is {}", subscriberName);
      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter listName is {}", listName);
      logger.debug("Parameter ownerName is {}", ownerName);
      logger.debug("Parameter ownerEmail is {}", ownerEmail);
      logger.debug("Parameter ownerLogo is {}", ownerLogo);

    // Based on Route#respond
    api.listRegistrationLetterConfirmationGet(routingContext, subscriberName, listGuid, listName, ownerName, ownerEmail, ownerLogo)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationLetterValidationGet(RoutingContext routingContext) {
    logger.info("listRegistrationLetterValidationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String subscriberName = requestParameters.queryParameter("subscriberName") != null ? requestParameters.queryParameter("subscriberName").getString() : null;
        String subscriberEmail = requestParameters.queryParameter("subscriberEmail") != null ? requestParameters.queryParameter("subscriberEmail").getString() : null;
        Boolean debug = requestParameters.queryParameter("debug") != null ? requestParameters.queryParameter("debug").getBoolean() : null;

      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter subscriberName is {}", subscriberName);
      logger.debug("Parameter subscriberEmail is {}", subscriberEmail);
      logger.debug("Parameter debug is {}", debug);

    // Based on Route#respond
    api.listRegistrationLetterValidationGet(routingContext, listGuid, subscriberName, subscriberEmail, debug)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationValidationGet(RoutingContext routingContext) {
    logger.info("listRegistrationValidationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String data = requestParameters.queryParameter("data") != null ? requestParameters.queryParameter("data").getString() : null;

      logger.debug("Parameter data is {}", data);

    // Based on Route#respond
    api.listRegistrationValidationGet(routingContext, data)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationsGet(RoutingContext routingContext) {
    logger.info("listRegistrationsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;

      logger.debug("Parameter listGuid is {}", listGuid);

    // Based on Route#respond
    api.listRegistrationsGet(routingContext, listGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
