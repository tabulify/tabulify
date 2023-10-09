package net.bytle.tower.eraldy.app.memberapp.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.app.memberapp.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.ListRegistrationPostBody;
import net.bytle.tower.eraldy.model.openapi.PasswordCredentials;
import net.bytle.tower.eraldy.model.openapi.PasswordOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthMemberappHandler {

private static final Logger logger = LoggerFactory.getLogger(AuthMemberappHandler.class);

private final AuthMemberapp api;

public AuthMemberappHandler(AuthMemberapp api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("loginEmailGet").handler(this::loginEmailGet);
    builder.operation("loginEmailPost").handler(this::loginEmailPost);
    builder.operation("loginGet").handler(this::loginGet);
    builder.operation("loginOauthAccessTokenGet").handler(this::loginOauthAccessTokenGet);
    builder.operation("loginOauthAuthorizeGet").handler(this::loginOauthAuthorizeGet);
    builder.operation("loginOauthProviderGet").handler(this::loginOauthProviderGet);
    builder.operation("loginPasswordGet").handler(this::loginPasswordGet);
    builder.operation("loginPasswordPost").handler(this::loginPasswordPost);
    builder.operation("loginPasswordResetGet").handler(this::loginPasswordResetGet);
    builder.operation("loginPasswordResetPost").handler(this::loginPasswordResetPost);
    builder.operation("loginPasswordUpdateGet").handler(this::loginPasswordUpdateGet);
    builder.operation("loginPasswordUpdatePost").handler(this::loginPasswordUpdatePost);
    builder.operation("logoutGet").handler(this::logoutGet);
    builder.operation("registerListConfirmationRegistrationGet").handler(this::registerListConfirmationRegistrationGet);
    builder.operation("registerListListGet").handler(this::registerListListGet);
    builder.operation("registerListPost").handler(this::registerListPost);
    builder.operation("registerUserGet").handler(this::registerUserGet);
    builder.operation("registerUserPost").handler(this::registerUserPost);
}

    private void loginEmailGet(RoutingContext routingContext) {
    logger.info("loginEmailGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String responseType = requestParameters.queryParameter("response_type") != null ? requestParameters.queryParameter("response_type").getString() : null;
        String state = requestParameters.queryParameter("state") != null ? requestParameters.queryParameter("state").getString() : null;
        String realmHandle = requestParameters.queryParameter("realm_handle") != null ? requestParameters.queryParameter("realm_handle").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter responseType is {}", responseType);
      logger.debug("Parameter state is {}", state);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.loginEmailGet(routingContext, redirectUri, clientId, responseType, state, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginEmailPost(RoutingContext routingContext) {
    logger.info("loginEmailPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  EmailIdentifier emailIdentifier = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<EmailIdentifier>(){}) : null;

      logger.debug("Parameter emailIdentifier is {}", emailIdentifier);

    // Based on Route#respond
    api.loginEmailPost(routingContext, emailIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginGet(RoutingContext routingContext) {
    logger.info("loginGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String responseType = requestParameters.queryParameter("response_type") != null ? requestParameters.queryParameter("response_type").getString() : null;
        String state = requestParameters.queryParameter("state") != null ? requestParameters.queryParameter("state").getString() : null;
        String realmHandle = requestParameters.queryParameter("realm_handle") != null ? requestParameters.queryParameter("realm_handle").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter responseType is {}", responseType);
      logger.debug("Parameter state is {}", state);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.loginGet(routingContext, redirectUri, clientId, responseType, state, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginOauthAccessTokenGet(RoutingContext routingContext) {
    logger.info("loginOauthAccessTokenGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String code = requestParameters.queryParameter("code") != null ? requestParameters.queryParameter("code").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String clientSecret = requestParameters.queryParameter("client_secret") != null ? requestParameters.queryParameter("client_secret").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter code is {}", code);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter clientSecret is {}", clientSecret);
      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.loginOauthAccessTokenGet(routingContext, code, clientId, clientSecret, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginOauthAuthorizeGet(RoutingContext routingContext) {
    logger.info("loginOauthAuthorizeGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String responseType = requestParameters.queryParameter("response_type") != null ? requestParameters.queryParameter("response_type").getString() : null;
        String state = requestParameters.queryParameter("state") != null ? requestParameters.queryParameter("state").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter responseType is {}", responseType);
      logger.debug("Parameter state is {}", state);

    // Based on Route#respond
    api.loginOauthAuthorizeGet(routingContext, redirectUri, clientId, responseType, state)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginOauthProviderGet(RoutingContext routingContext) {
    logger.info("loginOauthProviderGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String provider = requestParameters.pathParameter("provider") != null ? requestParameters.pathParameter("provider").getString() : null;
        String listGuid = requestParameters.queryParameter("list_guid") != null ? requestParameters.queryParameter("list_guid").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String realmHandle = requestParameters.queryParameter("realm_handle") != null ? requestParameters.queryParameter("realm_handle").getString() : null;
        String realmGuid = requestParameters.queryParameter("realm_guid") != null ? requestParameters.queryParameter("realm_guid").getString() : null;

      logger.debug("Parameter provider is {}", provider);
      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter realmHandle is {}", realmHandle);
      logger.debug("Parameter realmGuid is {}", realmGuid);

    // Based on Route#respond
    api.loginOauthProviderGet(routingContext, provider, listGuid, redirectUri, realmHandle, realmGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginPasswordGet(RoutingContext routingContext) {
    logger.info("loginPasswordGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String responseType = requestParameters.queryParameter("response_type") != null ? requestParameters.queryParameter("response_type").getString() : null;
        String state = requestParameters.queryParameter("state") != null ? requestParameters.queryParameter("state").getString() : null;
        String realmHandle = requestParameters.queryParameter("realm_handle") != null ? requestParameters.queryParameter("realm_handle").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter responseType is {}", responseType);
      logger.debug("Parameter state is {}", state);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.loginPasswordGet(routingContext, redirectUri, clientId, responseType, state, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginPasswordPost(RoutingContext routingContext) {
    logger.info("loginPasswordPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  PasswordCredentials passwordCredentials = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<PasswordCredentials>(){}) : null;

      logger.debug("Parameter passwordCredentials is {}", passwordCredentials);

    // Based on Route#respond
    api.loginPasswordPost(routingContext, passwordCredentials)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginPasswordResetGet(RoutingContext routingContext) {
    logger.info("loginPasswordResetGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String responseType = requestParameters.queryParameter("response_type") != null ? requestParameters.queryParameter("response_type").getString() : null;
        String state = requestParameters.queryParameter("state") != null ? requestParameters.queryParameter("state").getString() : null;
        String realmHandle = requestParameters.queryParameter("realm_handle") != null ? requestParameters.queryParameter("realm_handle").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter responseType is {}", responseType);
      logger.debug("Parameter state is {}", state);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.loginPasswordResetGet(routingContext, redirectUri, clientId, responseType, state, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginPasswordResetPost(RoutingContext routingContext) {
    logger.info("loginPasswordResetPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  EmailIdentifier emailIdentifier = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<EmailIdentifier>(){}) : null;

      logger.debug("Parameter emailIdentifier is {}", emailIdentifier);

    // Based on Route#respond
    api.loginPasswordResetPost(routingContext, emailIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginPasswordUpdateGet(RoutingContext routingContext) {
    logger.info("loginPasswordUpdateGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);



    // Based on Route#respond
    api.loginPasswordUpdateGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void loginPasswordUpdatePost(RoutingContext routingContext) {
    logger.info("loginPasswordUpdatePost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  PasswordOnly passwordOnly = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<PasswordOnly>(){}) : null;

      logger.debug("Parameter passwordOnly is {}", passwordOnly);

    // Based on Route#respond
    api.loginPasswordUpdatePost(routingContext, passwordOnly)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void logoutGet(RoutingContext routingContext) {
    logger.info("logoutGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.logoutGet(routingContext, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void registerListConfirmationRegistrationGet(RoutingContext routingContext) {
    logger.info("registerListConfirmationRegistrationGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String registrationGuid = requestParameters.pathParameter("registration_guid") != null ? requestParameters.pathParameter("registration_guid").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter registrationGuid is {}", registrationGuid);
      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.registerListConfirmationRegistrationGet(routingContext, registrationGuid, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void registerListListGet(RoutingContext routingContext) {
    logger.info("registerListListGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String listGuid = requestParameters.pathParameter("list_guid") != null ? requestParameters.pathParameter("list_guid").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter listGuid is {}", listGuid);
      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.registerListListGet(routingContext, listGuid, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void registerListPost(RoutingContext routingContext) {
    logger.info("registerListPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  ListRegistrationPostBody listRegistrationPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<ListRegistrationPostBody>(){}) : null;

      logger.debug("Parameter listRegistrationPostBody is {}", listRegistrationPostBody);

    // Based on Route#respond
    api.registerListPost(routingContext, listRegistrationPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void registerUserGet(RoutingContext routingContext) {
    logger.info("registerUserGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String clientId = requestParameters.queryParameter("client_id") != null ? requestParameters.queryParameter("client_id").getString() : null;
        String responseType = requestParameters.queryParameter("response_type") != null ? requestParameters.queryParameter("response_type").getString() : null;
        String state = requestParameters.queryParameter("state") != null ? requestParameters.queryParameter("state").getString() : null;
        String realmHandle = requestParameters.queryParameter("realm_handle") != null ? requestParameters.queryParameter("realm_handle").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter clientId is {}", clientId);
      logger.debug("Parameter responseType is {}", responseType);
      logger.debug("Parameter state is {}", state);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.registerUserGet(routingContext, redirectUri, clientId, responseType, state, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void registerUserPost(RoutingContext routingContext) {
    logger.info("registerUserPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  EmailIdentifier emailIdentifier = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<EmailIdentifier>(){}) : null;

      logger.debug("Parameter emailIdentifier is {}", emailIdentifier);

    // Based on Route#respond
    api.registerUserPost(routingContext, emailIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
