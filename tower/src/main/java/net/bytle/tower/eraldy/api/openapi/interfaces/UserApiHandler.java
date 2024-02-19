package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserApiHandler {

private static final Logger logger = LoggerFactory.getLogger(UserApiHandler.class);

private final UserApi api;

public UserApiHandler(UserApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("userMeGet").handler(this::userMeGet);
    builder.operation("userPost").handler(this::userPost);
    builder.operation("userUserIdentifierGet").handler(this::userUserIdentifierGet);
}

    private void userMeGet(RoutingContext routingContext) {
    logger.info("userMeGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.userMeGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void userPost(RoutingContext routingContext) {
    logger.info("userPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  UserPostBody userPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<UserPostBody>(){}) : null;

      logger.debug("Parameter userPostBody is {}", userPostBody);

    // Based on Route#respond
    api.userPost(routingContext, userPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void userUserIdentifierGet(RoutingContext routingContext) {
    logger.info("userUserIdentifierGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String userIdentifier = requestParameters.pathParameter("userIdentifier") != null ? requestParameters.pathParameter("userIdentifier").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realmIdentifier") != null ? requestParameters.queryParameter("realmIdentifier").getString() : null;

      logger.debug("Parameter userIdentifier is {}", userIdentifier);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);

    // Based on Route#respond
    api.userUserIdentifierGet(routingContext, userIdentifier, realmIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
