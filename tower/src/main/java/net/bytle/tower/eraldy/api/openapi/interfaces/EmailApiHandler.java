package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.json.JsonObject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map; // for pure json data

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
        Boolean failEarly = requestParameters.queryParameter("failEarly") != null ? requestParameters.queryParameter("failEarly").getBoolean() : true;

      logger.debug("Parameter address is {}", address);
      logger.debug("Parameter failEarly is {}", failEarly);

    // Based on Route#respond
    api.emailAddressAddressValidateGet(routingContext, address, failEarly)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
