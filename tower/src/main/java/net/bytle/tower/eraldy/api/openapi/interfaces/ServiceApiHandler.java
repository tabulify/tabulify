package net.bytle.tower.eraldy.api.openapi.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.ServiceSmtpPostBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceApiHandler {

private static final Logger logger = LoggerFactory.getLogger(ServiceApiHandler.class);

private final ServiceApi api;

public ServiceApiHandler(ServiceApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("serviceGet").handler(this::serviceGet);
    builder.operation("serviceSmtpPost").handler(this::serviceSmtpPost);
    builder.operation("servicesGet").handler(this::servicesGet);
}

    private void serviceGet(RoutingContext routingContext) {
    logger.info("serviceGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String serviceGuid = requestParameters.queryParameter("serviceGuid") != null ? requestParameters.queryParameter("serviceGuid").getString() : null;
        String serviceUri = requestParameters.queryParameter("serviceUri") != null ? requestParameters.queryParameter("serviceUri").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;
        String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;

      logger.debug("Parameter serviceGuid is {}", serviceGuid);
      logger.debug("Parameter serviceUri is {}", serviceUri);
      logger.debug("Parameter realmHandle is {}", realmHandle);
      logger.debug("Parameter realmGuid is {}", realmGuid);

    // Based on Route#respond
    api.serviceGet(routingContext, serviceGuid, serviceUri, realmHandle, realmGuid)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void serviceSmtpPost(RoutingContext routingContext) {
    logger.info("serviceSmtpPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  ServiceSmtpPostBody serviceSmtpPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<ServiceSmtpPostBody>(){}) : null;

      logger.debug("Parameter serviceSmtpPostBody is {}", serviceSmtpPostBody);

    // Based on Route#respond
    api.serviceSmtpPost(routingContext, serviceSmtpPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

    private void servicesGet(RoutingContext routingContext) {
    logger.info("servicesGet()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

            String realmGuid = requestParameters.queryParameter("realmGuid") != null ? requestParameters.queryParameter("realmGuid").getString() : null;
        String realmHandle = requestParameters.queryParameter("realmHandle") != null ? requestParameters.queryParameter("realmHandle").getString() : null;

      logger.debug("Parameter realmGuid is {}", realmGuid);
      logger.debug("Parameter realmHandle is {}", realmHandle);

    // Based on Route#respond
    api.servicesGet(routingContext, realmGuid, realmHandle)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
