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
    builder.operation("userAuthGet").handler(this::userAuthGet);
    builder.operation("userGet").handler(this::userGet);
    builder.operation("userGuidGet").handler(this::userGuidGet);
    builder.operation("userMeGet").handler(this::userMeGet);
    builder.operation("userPost").handler(this::userPost);
    builder.operation("usersGet").handler(this::usersGet);
}

    private void userAuthGet(RoutingContext routingContext) {
    logger.info("userAuthGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.userAuthGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void userGet(RoutingContext routingContext) {
    logger.info("userGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String userGuid = requestParameters.queryParameter("userGuid") != null ? requestParameters.queryParameter("userGuid").getString() : null;
        String userEmail = requestParameters.queryParameter("userEmail") != null ? requestParameters.queryParameter("userEmail").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;
        String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;

      logger.debug("Parameter userGuid is {}", userGuid);
      logger.debug("Parameter userEmail is {}", userEmail);
      logger.debug("Parameter realmHandle is {}", realmHandle);
      logger.debug("Parameter realmGuid is {}", realmGuid);

    // Based on Route#respond
    api.userGet(routingContext, userGuid, userEmail, realmHandle, realmGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void userGuidGet(RoutingContext routingContext) {
    logger.info("userGuidGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String guid = requestParameters.pathParameter("guid") != null ? requestParameters.pathParameter("guid").getString() : null;

      logger.debug("Parameter guid is {}", guid);

    // Based on Route#respond
    api.userGuidGet(routingContext, guid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
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

    private void usersGet(RoutingContext routingContext) {
    logger.info("usersGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter realmGuid is {}", realmGuid);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.usersGet(routingContext, realmGuid, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
