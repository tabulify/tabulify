package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.PasswordCredentials;
import net.bytle.tower.eraldy.model.openapi.PasswordOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthApiHandler {

private static final Logger logger = LoggerFactory.getLogger(AuthApiHandler.class);

private final AuthApi api;

public AuthApiHandler(AuthApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("authLoginAuthorizeGet").handler(this::authLoginAuthorizeGet);
    builder.operation("authLoginEmailPost").handler(this::authLoginEmailPost);
    builder.operation("authLoginOauthAccessTokenGet").handler(this::authLoginOauthAccessTokenGet);
    builder.operation("authLoginOauthProviderGet").handler(this::authLoginOauthProviderGet);
    builder.operation("authLoginPasswordPost").handler(this::authLoginPasswordPost);
    builder.operation("authLoginPasswordResetPost").handler(this::authLoginPasswordResetPost);
    builder.operation("authLoginPasswordUpdatePost").handler(this::authLoginPasswordUpdatePost);
    builder.operation("authLogoutGet").handler(this::authLogoutGet);
    builder.operation("authUserRegisterPost").handler(this::authUserRegisterPost);
}

    private void authLoginAuthorizeGet(RoutingContext routingContext) {
    logger.info("authLoginAuthorizeGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.authLoginAuthorizeGet(routingContext, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLoginEmailPost(RoutingContext routingContext) {
    logger.info("authLoginEmailPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  EmailIdentifier emailIdentifier = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<EmailIdentifier>(){}) : null;

      logger.debug("Parameter emailIdentifier is {}", emailIdentifier);

    // Based on Route#respond
    api.authLoginEmailPost(routingContext, emailIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLoginOauthAccessTokenGet(RoutingContext routingContext) {
    logger.info("authLoginOauthAccessTokenGet()");

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
    api.authLoginOauthAccessTokenGet(routingContext, code, clientId, clientSecret, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLoginOauthProviderGet(RoutingContext routingContext) {
    logger.info("authLoginOauthProviderGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String provider = requestParameters.pathParameter("provider") != null ? requestParameters.pathParameter("provider").getString() : null;
        String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;
        String realmIdentifier = requestParameters.queryParameter("realm_identifier") != null ? requestParameters.queryParameter("realm_identifier").getString() : null;
        String listGuid = requestParameters.queryParameter("list_guid") != null ? requestParameters.queryParameter("list_guid").getString() : null;

      logger.debug("Parameter provider is {}", provider);
      logger.debug("Parameter redirectUri is {}", redirectUri);
      logger.debug("Parameter realmIdentifier is {}", realmIdentifier);
      logger.debug("Parameter listGuid is {}", listGuid);

    // Based on Route#respond
    api.authLoginOauthProviderGet(routingContext, provider, redirectUri, realmIdentifier, listGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLoginPasswordPost(RoutingContext routingContext) {
    logger.info("authLoginPasswordPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  PasswordCredentials passwordCredentials = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<PasswordCredentials>(){}) : null;

      logger.debug("Parameter passwordCredentials is {}", passwordCredentials);

    // Based on Route#respond
    api.authLoginPasswordPost(routingContext, passwordCredentials)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLoginPasswordResetPost(RoutingContext routingContext) {
    logger.info("authLoginPasswordResetPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  EmailIdentifier emailIdentifier = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<EmailIdentifier>(){}) : null;

      logger.debug("Parameter emailIdentifier is {}", emailIdentifier);

    // Based on Route#respond
    api.authLoginPasswordResetPost(routingContext, emailIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLoginPasswordUpdatePost(RoutingContext routingContext) {
    logger.info("authLoginPasswordUpdatePost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  PasswordOnly passwordOnly = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<PasswordOnly>(){}) : null;

      logger.debug("Parameter passwordOnly is {}", passwordOnly);

    // Based on Route#respond
    api.authLoginPasswordUpdatePost(routingContext, passwordOnly)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authLogoutGet(RoutingContext routingContext) {
    logger.info("authLogoutGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String redirectUri = requestParameters.queryParameter("redirect_uri") != null ? requestParameters.queryParameter("redirect_uri").getString() : null;

      logger.debug("Parameter redirectUri is {}", redirectUri);

    // Based on Route#respond
    api.authLogoutGet(routingContext, redirectUri)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void authUserRegisterPost(RoutingContext routingContext) {
    logger.info("authUserRegisterPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  EmailIdentifier emailIdentifier = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<EmailIdentifier>(){}) : null;

      logger.debug("Parameter emailIdentifier is {}", emailIdentifier);

    // Based on Route#respond
    api.authUserRegisterPost(routingContext, emailIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
