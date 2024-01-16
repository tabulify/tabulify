package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Campaign;
import net.bytle.tower.eraldy.model.openapi.CampaignEmailPostBody;

public interface CampaignApi  {

    /**
     * Create an email campaign
    */
    Future<ApiResponse<Campaign>> campaignEmailPost(RoutingContext routingContext, CampaignEmailPostBody campaignEmailPostBody);
}
