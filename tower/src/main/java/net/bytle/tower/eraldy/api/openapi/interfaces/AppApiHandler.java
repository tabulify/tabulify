package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.ListBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppApiHandler {

private static final Logger logger = LoggerFactory.getLogger(AppApiHandler.class);

private final AppApi api;

public AppApiHandler(AppApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("appAppIdentifierListsGet").handler(this::appAppIdentifierListsGet);
    builder.operation("appAppListPost").handler(this::appAppListPost);
    builder.operation("appGet").handler(this::appGet);
    builder.operation("appPost").handler(this::appPost);
    builder.operation("appsGet").handler(this::appsGet);
}

    private void appAppIdentifierListsGet(RoutingContext routingContext) {
    logger.info("appAppIdentifierListsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String appIdentifier = requestParameters.pathParameter("appIdentifier") != null ? requestParameters.pathParameter("appIdentifier").getString() : null;

      logger.debug("Parameter appIdentifier is {}", appIdentifier);

    // Based on Route#respond
    api.appAppIdentifierListsGet(routingContext, appIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void appAppListPost(RoutingContext routingContext) {
    logger.info("appAppListPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String appIdentifier = requestParameters.pathParameter("appIdentifier") != null ? requestParameters.pathParameter("appIdentifier").getString() : null;
  RequestParameter requestParameterBody = requestParameters.body();
  ListBody listBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<ListBody>(){}) : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter appIdentifier is {}", appIdentifier);
      logger.debug("Parameter listBody is {}", listBody);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.appAppListPost(routingContext, appIdentifier, listBody, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void appGet(RoutingContext routingContext) {
    logger.info("appGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String appIdentifier = requestParameters.pathParameter("appIdentifier") != null ? requestParameters.pathParameter("appIdentifier").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter appIdentifier is {}", appIdentifier);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.appGet(routingContext, appIdentifier, realmIdentifier)
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

            String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.appsGet(routingContext, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
