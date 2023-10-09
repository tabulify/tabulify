package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppComboprivateapiHandler {

private static final Logger logger = LoggerFactory.getLogger(AppComboprivateapiHandler.class);

private final AppComboprivateapi api;

public AppComboprivateapiHandler(AppComboprivateapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("appGet").handler(this::appGet);
    builder.operation("appPost").handler(this::appPost);
    builder.operation("appsGet").handler(this::appsGet);
}

    private void appGet(RoutingContext routingContext) {
    logger.info("appGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String appGuid = requestParameters.queryParameter("appGuid") != null ? requestParameters.queryParameter("appGuid").getString() : null;
        String appUri = requestParameters.queryParameter("appUri") != null ? requestParameters.queryParameter("appUri").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;
        String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;

      logger.debug("Parameter appGuid is {}", appGuid);
      logger.debug("Parameter appUri is {}", appUri);
      logger.debug("Parameter realmHandle is {}", realmHandle);
      logger.debug("Parameter realmGuid is {}", realmGuid);

    // Based on Route#respond
    api.appGet(routingContext, appGuid, appUri, realmHandle, realmGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void appPost(RoutingContext routingContext) {
    logger.info("appPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  AppPostBody appPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<AppPostBody>(){}) : null;

      logger.debug("Parameter appPostBody is {}", appPostBody);

    // Based on Route#respond
    api.appPost(routingContext, appPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void appsGet(RoutingContext routingContext) {
    logger.info("appsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter realmGuid is {}", realmGuid);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.appsGet(routingContext, realmGuid, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
