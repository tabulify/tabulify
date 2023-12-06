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
import net.bytle.tower.eraldy.model.openapi.ListPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListApiHandler {

private static final Logger logger = LoggerFactory.getLogger(ListApiHandler.class);

private final ListApi api;

public ListApiHandler(ListApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("listImportPost").handler(this::listImportPost);
    builder.operation("listListDelete").handler(this::listListDelete);
    builder.operation("listListGet").handler(this::listListGet);
    builder.operation("listPost").handler(this::listPost);
    builder.operation("listRegisterConfirmationRegistrationGet").handler(this::listRegisterConfirmationRegistrationGet);
    builder.operation("listRegistrationGet").handler(this::listRegistrationGet);
    builder.operation("listRegistrationLetterConfirmationGet").handler(this::listRegistrationLetterConfirmationGet);
    builder.operation("listRegistrationLetterValidationGet").handler(this::listRegistrationLetterValidationGet);
    builder.operation("listRegistrationValidationGet").handler(this::listRegistrationValidationGet);
    builder.operation("listRegistrationsGet").handler(this::listRegistrationsGet);
    builder.operation("listsGet").handler(this::listsGet);
    builder.operation("listsSummaryGet").handler(this::listsSummaryGet);
}

    private void listImportPost(RoutingContext routingContext) {
    logger.info("listImportPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listIdentifier = requestParameters.pathParameter("listIdentifier") != null ? requestParameters.pathParameter("listIdentifier").getString() : null;
        FileUpload fileBinary = routingContext.fileUploads().iterator().next();

      logger.debug("Parameter listIdentifier is {}", listIdentifier);
      logger.debug("Parameter fileBinary is {}", fileBinary);

    // Based on Route#respond
    api.listImportPost(routingContext, listIdentifier, fileBinary)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
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

    private void listRegisterConfirmationRegistrationGet(RoutingContext routingContext) {
    logger.info("listRegisterConfirmationRegistrationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String registrationGuid = requestParameters.pathParameter("registration_guid") != null ? requestParameters.pathParameter("registration_guid").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter registrationGuid is {}", registrationGuid);
      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.listRegisterConfirmationRegistrationGet(routingContext, registrationGuid, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationGet(RoutingContext routingContext) {
    logger.info("listRegistrationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String guid = requestParameters.queryParameter("guid") != null ? requestParameters.queryParameter("guid").getString() : null;
        String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;
        String subscriberEmail = requestParameters.queryParameter("subscriberEmail") != null ? requestParameters.queryParameter("subscriberEmail").getString() : null;

      logger.debug("Parameter guid is {}", guid);
      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter subscriberEmail is {}", subscriberEmail);

    // Based on Route#respond
    api.listRegistrationGet(routingContext, guid, listGuid, subscriberEmail)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationLetterConfirmationGet(RoutingContext routingContext) {
    logger.info("listRegistrationLetterConfirmationGet()");

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
    api.listRegistrationLetterConfirmationGet(routingContext, subscriberName, listGuid, listName, ownerName, ownerEmail, ownerLogo)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationLetterValidationGet(RoutingContext routingContext) {
    logger.info("listRegistrationLetterValidationGet()");

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
    api.listRegistrationLetterValidationGet(routingContext, listGuid, subscriberName, subscriberEmail, debug)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationValidationGet(RoutingContext routingContext) {
    logger.info("listRegistrationValidationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String data = requestParameters.queryParameter("data") != null ? requestParameters.queryParameter("data").getString() : null;

      logger.debug("Parameter data is {}", data);

    // Based on Route#respond
    api.listRegistrationValidationGet(routingContext, data)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void listRegistrationsGet(RoutingContext routingContext) {
    logger.info("listRegistrationsGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listGuid = requestParameters.queryParameter("listGuid") != null ? requestParameters.queryParameter("listGuid").getString() : null;

      logger.debug("Parameter listGuid is {}", listGuid);

    // Based on Route#respond
    api.listRegistrationsGet(routingContext, listGuid)
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
