package net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.CspObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CspPublicapiHandler {

private static final Logger logger = LoggerFactory.getLogger(CspPublicapiHandler.class);

private final CspPublicapi api;

public CspPublicapiHandler(CspPublicapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("cspReportPost").handler(this::cspReportPost);
}

    private void cspReportPost(RoutingContext routingContext) {
    logger.info("cspReportPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  CspObject cspObject = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<CspObject>(){}) : null;

      logger.debug("Parameter cspObject is {}", cspObject);

    // Based on Route#respond
    api.cspReportPost(routingContext, cspObject)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
