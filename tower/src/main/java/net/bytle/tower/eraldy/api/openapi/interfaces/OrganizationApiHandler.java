package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map; // for pure json data

public class OrganizationApiHandler {

private static final Logger logger = LoggerFactory.getLogger(OrganizationApiHandler.class);

private final OrganizationApi api;

public OrganizationApiHandler(OrganizationApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("orgaOrgaUsersGet").handler(this::orgaOrgaUsersGet);
    builder.operation("organizationUserMeGet").handler(this::organizationUserMeGet);
}

    private void orgaOrgaUsersGet(RoutingContext routingContext) {
    logger.info("orgaOrgaUsersGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String orgaIdentifier = requestParameters.pathParameter("orgaIdentifier") != null ? requestParameters.pathParameter("orgaIdentifier").getString() : null;

      logger.debug("Parameter orgaIdentifier is {}", orgaIdentifier);

    // Based on Route#respond
    api.orgaOrgaUsersGet(routingContext, orgaIdentifier)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void organizationUserMeGet(RoutingContext routingContext) {
    logger.info("organizationUserMeGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);


      // Based on Route#respond
    api.organizationUserMeGet(routingContext)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
