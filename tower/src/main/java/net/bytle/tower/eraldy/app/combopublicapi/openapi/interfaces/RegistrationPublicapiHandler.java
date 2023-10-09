package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.ListRegistrationPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrationPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(RegistrationPublicapiHandler.class);

private final RegistrationPublicapi api;

public RegistrationPublicapiHandler(RegistrationPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("listRegistrationPost").handler(this::listRegistrationPost);
}

    private void listRegistrationPost(RoutingContext routingContext) {
    logger.info("listRegistrationPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  ListRegistrationPostBody listRegistrationPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<ListRegistrationPostBody>(){}) : null;

      logger.debug("Parameter listRegistrationPostBody is {}", listRegistrationPostBody);

    // Based on Route#respond
    api.listRegistrationPost(routingContext, listRegistrationPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
