package net.bytle.tower.eraldy.api.openapi.interfaces;

import net.bytle.tower.eraldy.model.openapi.Campaign;
import net.bytle.tower.eraldy.model.openapi.CampaignEmailPostBody;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface CampaignApi  {

  /**
     * Create an email campaign
    */
    Future<ApiResponse<Campaign>> campaignEmailPost(RoutingContext routingContext, CampaignEmailPostBody campaignEmailPostBody);
}
