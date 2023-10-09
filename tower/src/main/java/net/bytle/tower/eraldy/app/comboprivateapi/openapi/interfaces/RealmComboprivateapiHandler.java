package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.RealmPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmComboprivateapiHandler {

private static final Logger logger = LoggerFactory.getLogger(RealmComboprivateapiHandler.class);

private final RealmComboprivateapi api;

public RealmComboprivateapiHandler(RealmComboprivateapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("realmGet").handler(this::realmGet);
    builder.operation("realmPost").handler(this::realmPost);
    builder.operation("realmUsersNewGet").handler(this::realmUsersNewGet);
    builder.operation("realmsGet").handler(this::realmsGet);
    builder.operation("realmsOwnedByGet").handler(this::realmsOwnedByGet);
}

    private void realmGet(RoutingContext routingContext) {
    logger.info("realmGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter realmGuid is {}", realmGuid);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.realmGet(routingContext, realmGuid, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void realmPost(RoutingContext routingContext) {
    logger.info("realmPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  RealmPostBody realmPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<RealmPostBody>(){}) : null;

      logger.debug("Parameter realmPostBody is {}", realmPostBody);

    // Based on Route#respond
    api.realmPost(routingContext, realmPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void realmUsersNewGet(RoutingContext routingContext) {
    logger.info("realmUsersNewGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmGuid = requestParameters.pathParameter("realmGuid") != null ? requestParameters.pathParameter("realmGuid").getString() : null;

      logger.debug("Parameter realmGuid is {}", realmGuid);

    // Based on Route#respond
    api.realmUsersNewGet(routingContext, realmGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void realmsGet(RoutingContext routingContext) {
    logger.info("realmsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.realmsGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void realmsOwnedByGet(RoutingContext routingContext) {
    logger.info("realmsOwnedByGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String userGuid = requestParameters.pathParameter("userGuid") != null ? requestParameters.pathParameter("userGuid").getString() : null;

      logger.debug("Parameter userGuid is {}", userGuid);

    // Based on Route#respond
    api.realmsOwnedByGet(routingContext, userGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
