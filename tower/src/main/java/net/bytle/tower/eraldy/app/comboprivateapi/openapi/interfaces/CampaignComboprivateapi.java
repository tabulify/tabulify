package net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Campaign;
import net.bytle.tower.eraldy.model.openapi.CampaignEmailPostBody;

public interface CampaignComboprivateapi  {
    Future<ApiResponse<Campaign>> campaignEmailPost(RoutingContext routingContext, CampaignEmailPostBody campaignEmailPostBody);
}
