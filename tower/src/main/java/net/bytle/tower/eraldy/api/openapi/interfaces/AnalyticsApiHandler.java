package net.bytle.tower.eraldy.api.openapi.interfaces;

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

import java.util.Map;

public class AnalyticsApiHandler {

private static final Logger logger = LoggerFactory.getLogger(AnalyticsApiHandler.class);

private final AnalyticsApi api;

public AnalyticsApiHandler(AnalyticsApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("analyticsEventNameGet").handler(this::analyticsEventNameGet);
    builder.operation("analyticsEventPost").handler(this::analyticsEventPost);
}

    private void analyticsEventNameGet(RoutingContext routingContext) {
    logger.info("analyticsEventNameGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String eventName = requestParameters.pathParameter("eventName") != null ? requestParameters.pathParameter("eventName").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;
        String appIdentifier = requestParameters.queryParameter("appIdentifier") != null ? requestParameters.queryParameter("appIdentifier").getString() : null;

      logger.debug("Parameter eventName is {}", eventName);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);
      logger.debug("Parameter appIdentifier is {}", appIdentifier);

    // Based on Route#respond
    api.analyticsEventNameGet(routingContext, eventName, realmIdentifier, appIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void analyticsEventPost(RoutingContext routingContext) {
    logger.info("analyticsEventPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  Map<String, Object> requestBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<Map<String, Object>>(){}) : null;

      logger.debug("Parameter requestBody is {}", requestBody);

    // Based on Route#respond
    api.analyticsEventPost(routingContext, requestBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
