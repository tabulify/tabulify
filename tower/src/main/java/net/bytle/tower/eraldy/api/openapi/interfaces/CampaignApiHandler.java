package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.Campaign;
import net.bytle.tower.eraldy.model.openapi.CampaignEmailPostBody;

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

public class CampaignApiHandler {

private static final Logger logger = LoggerFactory.getLogger(CampaignApiHandler.class);

private final CampaignApi api;

public CampaignApiHandler(CampaignApi api) {
this.api = api;
}

public void mount(RouterBuilder builder) {
    builder.operation("campaignEmailPost").handler(this::campaignEmailPost);
}

    private void campaignEmailPost(RoutingContext routingContext) {
    logger.info("campaignEmailPost()");

    // Param extraction
    RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

      RequestParameter requestParameterBody = requestParameters.body();
  CampaignEmailPostBody campaignEmailPostBody = requestParameterBody != null ? DatabindCodec.mapper().convertValue(requestParameterBody.get(), new TypeReference<CampaignEmailPostBody>(){}) : null;

      logger.debug("Parameter campaignEmailPostBody is {}", campaignEmailPostBody);

    // Based on Route#respond
    api.campaignEmailPost(routingContext, campaignEmailPostBody)
    .onSuccess(apiResponse -> ApiVertxSupport.respond(routingContext, apiResponse))
    .onFailure(routingContext::fail);
    }

}
