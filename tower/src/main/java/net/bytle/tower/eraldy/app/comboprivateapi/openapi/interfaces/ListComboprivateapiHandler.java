package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.ListPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListComboprivateapiHandler {

private static final Logger logger = LoggerFactory.getLogger(ListComboprivateapiHandler.class);

private final ListComboprivateapi api;

public ListComboprivateapiHandler(ListComboprivateapi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("listGet").handler(this::listGet);
    builder.operation("listPost").handler(this::listPost);
    builder.operation("listsGet").handler(this::listsGet);
    builder.operation("listsSummaryGet").handler(this::listsSummaryGet);
}

    private void listGet(RoutingContext routingContext) {
    logger.info("listGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String listHandle = requestParameters.queryParameter("listHandle") != null ? requestParameters.queryParameter("listHandle").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter listHandle is {}", listHandle);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.listGet(routingContext, listGuid, listHandle, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listPost(RoutingContext routingContext) {
    logger.info("listPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  ListPostBody listPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<ListPostBody>(){}) : null;

      logger.debug("Parameter listPostBody is {}", listPostBody);

    // Based on Route#respond
    api.listPost(routingContext, listPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listsGet(RoutingContext routingContext) {
    logger.info("listsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String appGuid = requestParameters.queryParameter("appGuid") != null ? requestParameters.queryParameter("appGuid").getString() : null;
        String appUri = requestParameters.queryParameter("appUri") != null ? requestParameters.queryParameter("appUri").getString() : null;
        String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter appGuid is {}", appGuid);
      logger.debug("Parameter appUri is {}", appUri);
      logger.debug("Parameter realmGuid is {}", realmGuid);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.listsGet(routingContext, appGuid, appUri, realmGuid, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listsSummaryGet(RoutingContext routingContext) {
    logger.info("listsSummaryGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter realmGuid is {}", realmGuid);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.listsSummaryGet(routingContext, realmGuid, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
