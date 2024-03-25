package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.MailingUpdatePost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailingApiHandler {

private static final Logger logger = LoggerFactory.getLogger(MailingApiHandler.class);

private final MailingApi api;

public MailingApiHandler(MailingApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("mailingIdentifierEmailGet").handler(this::mailingIdentifierEmailGet);
    builder.operation("mailingIdentifierEmailPost").handler(this::mailingIdentifierEmailPost);
    builder.operation("mailingIdentifierGet").handler(this::mailingIdentifierGet);
    builder.operation("mailingIdentifierPost").handler(this::mailingIdentifierPost);
}

    private void mailingIdentifierEmailGet(RoutingContext routingContext) {
    logger.info("mailingIdentifierEmailGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String mailingIdentifier = requestParameters.pathParameter("mailingIdentifier") != null ? requestParameters.pathParameter("mailingIdentifier").getString() : null;

      logger.debug("Parameter mailingIdentifier is {}", mailingIdentifier);

    // Based on Route#respond
    api.mailingIdentifierEmailGet(routingContext, mailingIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void mailingIdentifierEmailPost(RoutingContext routingContext) {
    logger.info("mailingIdentifierEmailPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String mailingIdentifier = requestParameters.pathParameter("mailingIdentifier") != null ? requestParameters.pathParameter("mailingIdentifier").getString() : null;
  RequestParameter requestParameterBody = requestParameters.body();
  MailingUpdatePost mailingUpdatePost = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<MailingUpdatePost>(){}) : null;

      logger.debug("Parameter mailingIdentifier is {}", mailingIdentifier);
      logger.debug("Parameter mailingUpdatePost is {}", mailingUpdatePost);

    // Based on Route#respond
    api.mailingIdentifierEmailPost(routingContext, mailingIdentifier, mailingUpdatePost)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void mailingIdentifierGet(RoutingContext routingContext) {
    logger.info("mailingIdentifierGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String mailingIdentifier = requestParameters.pathParameter("mailingIdentifier") != null ? requestParameters.pathParameter("mailingIdentifier").getString() : null;

      logger.debug("Parameter mailingIdentifier is {}", mailingIdentifier);

    // Based on Route#respond
    api.mailingIdentifierGet(routingContext, mailingIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void mailingIdentifierPost(RoutingContext routingContext) {
    logger.info("mailingIdentifierPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String mailingIdentifier = requestParameters.pathParameter("mailingIdentifier") != null ? requestParameters.pathParameter("mailingIdentifier").getString() : null;
  RequestParameter requestParameterBody = requestParameters.body();
  MailingUpdatePost mailingUpdatePost = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<MailingUpdatePost>(){}) : null;

      logger.debug("Parameter mailingIdentifier is {}", mailingIdentifier);
      logger.debug("Parameter mailingUpdatePost is {}", mailingUpdatePost);

    // Based on Route#respond
    api.mailingIdentifierPost(routingContext, mailingIdentifier, mailingUpdatePost)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
