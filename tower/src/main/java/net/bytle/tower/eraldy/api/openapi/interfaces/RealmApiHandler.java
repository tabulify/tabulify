package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.RealmPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmApiHandler {

private static final Logger logger = LoggerFactory.getLogger(RealmApiHandler.class);

private final RealmApi api;

public RealmApiHandler(RealmApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("realmGet").handler(this::realmGet);
    builder.operation("realmPost").handler(this::realmPost);
    builder.operation("realmRealmUsersGet").handler(this::realmRealmUsersGet);
    builder.operation("realmsGet").handler(this::realmsGet);
    builder.operation("realmsOwnedByGet").handler(this::realmsOwnedByGet);
    builder.operation("realmsOwnedByMeGet").handler(this::realmsOwnedByMeGet);
}

    private void realmGet(RoutingContext routingContext) {
    logger.info("realmGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmIdentifier = requestParameters.pathParameter("realmIdentifier") != null ? requestParameters.pathParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.realmGet(routingContext, realmIdentifier)
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

    private void realmRealmUsersGet(RoutingContext routingContext) {
    logger.info("realmRealmUsersGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmIdentifier = requestParameters.pathParameter("realmIdentifier") != null ? requestParameters.pathParameter("realmIdentifier").getString() : null;
        Long pageSize = requestParameters.queryParameter("pageSize") != null ? requestParameters.queryParameter("pageSize").getLong() : null;
        Long pageId = requestParameters.queryParameter("pageId") != null ? requestParameters.queryParameter("pageId").getLong() : null;
        String searchTerm = requestParameters.queryParameter("searchTerm") != null ? requestParameters.queryParameter("searchTerm").getString() : null;

      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);
      logger.debug("Parameter pageSize is {}", pageSize);
      logger.debug("Parameter pageId is {}", pageId);
      logger.debug("Parameter searchTerm is {}", searchTerm);

    // Based on Route#respond
    api.realmRealmUsersGet(routingContext, realmIdentifier, pageSize, pageId, searchTerm)
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

    private void realmsOwnedByMeGet(RoutingContext routingContext) {
    logger.info("realmsOwnedByMeGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.realmsOwnedByMeGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
