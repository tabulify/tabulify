package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.ListBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListApiHandler {

private static final Logger logger = LoggerFactory.getLogger(ListApiHandler.class);

private final ListApi api;

public ListApiHandler(ListApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("listListDelete").handler(this::listListDelete);
    builder.operation("listListGet").handler(this::listListGet);
    builder.operation("listListImportJobDetailsGet").handler(this::listListImportJobDetailsGet);
    builder.operation("listListImportJobGet").handler(this::listListImportJobGet);
    builder.operation("listListImportPost").handler(this::listListImportPost);
    builder.operation("listListImportsGet").handler(this::listListImportsGet);
    builder.operation("listListPatch").handler(this::listListPatch);
    builder.operation("listListUsersGet").handler(this::listListUsersGet);
    builder.operation("listUserConfirmationUserGet").handler(this::listUserConfirmationUserGet);
    builder.operation("listUserIdentifierGet").handler(this::listUserIdentifierGet);
    builder.operation("listUserLetterConfirmationGet").handler(this::listUserLetterConfirmationGet);
    builder.operation("listUserLetterValidationGet").handler(this::listUserLetterValidationGet);
    builder.operation("listsGet").handler(this::listsGet);
    builder.operation("listsSummaryGet").handler(this::listsSummaryGet);
}

    private void listListDelete(RoutingContext routingContext) {
    logger.info("listListDelete()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.listListDelete(routingContext, listIdentifier, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListGet(RoutingContext routingContext) {
    logger.info("listListGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.listListGet(routingContext, listIdentifier, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListImportJobDetailsGet(RoutingContext routingContext) {
    logger.info("listListImportJobDetailsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        String jobIdentifier = requestParameters.pathParameter("jobIdentifier") != null ? requestParameters.pathParameter("jobIdentifier").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter jobIdentifier is {}", jobIdentifier);

    // Based on Route#respond
    api.listListImportJobDetailsGet(routingContext, listIdentifier, jobIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListImportJobGet(RoutingContext routingContext) {
    logger.info("listListImportJobGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        String jobIdentifier = requestParameters.pathParameter("jobIdentifier") != null ? requestParameters.pathParameter("jobIdentifier").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter jobIdentifier is {}", jobIdentifier);

    // Based on Route#respond
    api.listListImportJobGet(routingContext, listIdentifier, jobIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListImportPost(RoutingContext routingContext) {
    logger.info("listListImportPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        Integer rowCountToProcess = requestParameters.queryParameter("rowCountToProcess") != null ? requestParameters.queryParameter("rowCountToProcess").getInteger() : 10000;
        FileUpload fileBinary = routingContext.fileUploads().iterator().next();

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter rowCountToProcess is {}", rowCountToProcess);
      logger.debug("Parameter fileBinary is {}", fileBinary);

    // Based on Route#respond
    api.listListImportPost(routingContext, listIdentifier, rowCountToProcess, fileBinary)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListImportsGet(RoutingContext routingContext) {
    logger.info("listListImportsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);

    // Based on Route#respond
    api.listListImportsGet(routingContext, listIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListPatch(RoutingContext routingContext) {
    logger.info("listListPatch()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
  RequestParameter requestParameterBody = requestParameters.body();
  ListBody listBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<ListBody>(){}) : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter listBody is {}", listBody);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.listListPatch(routingContext, listIdentifier, listBody, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listListUsersGet(RoutingContext routingContext) {
    logger.info("listListUsersGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        Long pageSize = requestParameters.queryParameter("pageSize") != null ? requestParameters.queryParameter("pageSize").getLong() : null;
        Long pageId = requestParameters.queryParameter("pageId") != null ? requestParameters.queryParameter("pageId").getLong() : null;
        String searchTerm = requestParameters.queryParameter("searchTerm") != null ? requestParameters.queryParameter("searchTerm").getString() : null;

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter pageSize is {}", pageSize);
      logger.debug("Parameter pageId is {}", pageId);
      logger.debug("Parameter searchTerm is {}", searchTerm);

    // Based on Route#respond
    api.listListUsersGet(routingContext, listIdentifier, pageSize, pageId, searchTerm)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listUserConfirmationUserGet(RoutingContext routingContext) {
    logger.info("listUserConfirmationUserGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listUserIdentifier = requestParameters.pathParameter("listUserIdentifier") != null ? requestParameters.pathParameter("listUserIdentifier").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter listUserIdentifier is {}", listUserIdentifier);
      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.listUserConfirmationUserGet(routingContext, listUserIdentifier, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listUserIdentifierGet(RoutingContext routingContext) {
    logger.info("listUserIdentifierGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listUserIdentifier = requestParameters.pathParameter("listUserIdentifier") != null ? requestParameters.pathParameter("listUserIdentifier").getString() : null;

      logger.debug("Parameter listUserIdentifier is {}", listUserIdentifier);

    // Based on Route#respond
    api.listUserIdentifierGet(routingContext, listUserIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listUserLetterConfirmationGet(RoutingContext routingContext) {
    logger.info("listUserLetterConfirmationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String subscriberName = requestParameters.queryParameter("subscriberName") != null ? requestParameters.queryParameter("subscriberName").getString() : null;
        String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String listName = requestParameters.queryParameter("listName") != null ? requestParameters.queryParameter("listName").getString() : null;
        String ownerName = requestParameters.queryParameter("ownerName") != null ? requestParameters.queryParameter("ownerName").getString() : null;
        String ownerEmail = requestParameters.queryParameter("ownerEmail") != null ? requestParameters.queryParameter("ownerEmail").getString() : null;
        String ownerLogo = requestParameters.queryParameter("ownerLogo") != null ? requestParameters.queryParameter("ownerLogo").getString() : null;

      logger.debug("Parameter subscriberName is {}", subscriberName);
      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter listName is {}", listName);
      logger.debug("Parameter ownerName is {}", ownerName);
      logger.debug("Parameter ownerEmail is {}", ownerEmail);
      logger.debug("Parameter ownerLogo is {}", ownerLogo);

    // Based on Route#respond
    api.listUserLetterConfirmationGet(routingContext, subscriberName, listGuid, listName, ownerName, ownerEmail, ownerLogo)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listUserLetterValidationGet(RoutingContext routingContext) {
    logger.info("listUserLetterValidationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String subscriberName = requestParameters.queryParameter("subscriberName") != null ? requestParameters.queryParameter("subscriberName").getString() : null;
        String subscriberEmail = requestParameters.queryParameter("subscriberEmail") != null ? requestParameters.queryParameter("subscriberEmail").getString() : null;
        Boolean debug = requestParameters.queryParameter("debug") != null ? requestParameters.queryParameter("debug").getBoolean() : null;

      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter subscriberName is {}", subscriberName);
      logger.debug("Parameter subscriberEmail is {}", subscriberEmail);
      logger.debug("Parameter debug is {}", debug);

    // Based on Route#respond
    api.listUserLetterValidationGet(routingContext, listGuid, subscriberName, subscriberEmail, debug)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listsGet(RoutingContext routingContext) {
    logger.info("listsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String appGuid = requestParameters.queryParameter("appGuid") != null ? requestParameters.queryParameter("appGuid").getString() : null;
        String appUri = requestParameters.queryParameter("appUri") != null ? requestParameters.queryParameter("appUri").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter appGuid is {}", appGuid);
      logger.debug("Parameter appUri is {}", appUri);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.listsGet(routingContext, appGuid, appUri, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listsSummaryGet(RoutingContext routingContext) {
    logger.info("listsSummaryGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.listsSummaryGet(routingContext, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
