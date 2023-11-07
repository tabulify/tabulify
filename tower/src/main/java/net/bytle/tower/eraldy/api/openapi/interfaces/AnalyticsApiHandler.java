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
    builder.operation("analyticsEventPost")
      .handler(this::analyticsEventPost);
}

    private void analyticsEventPost(RoutingContext routingContext) {
    logger.info("analyticsEventPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  Map<String, Object> requestBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<>() {
  }) : null;

      logger.debug("Parameter requestBody is {}", requestBody);

    // Based on Route#respond
    api.analyticsEventPost(routingContext, requestBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
