package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.CspObject;

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

public class CspApiHandler {

private static final Logger logger = LoggerFactory.getLogger(CspApiHandler.class);

private final CspApi api;

public CspApiHandler(CspApi api) {
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
