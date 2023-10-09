package net.bytle.tower.eraldy.app.comboprivateapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces.CampaignComboprivateapi;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Campaign;
import net.bytle.tower.eraldy.model.openapi.CampaignEmailPostBody;

public class CampaignComboprivateapiImpl implements CampaignComboprivateapi {

  @Override
  public Future<ApiResponse<Campaign>> campaignEmailPost(RoutingContext routingContext, CampaignEmailPostBody campaignEmailPostBody) {
    throw new InternalException("Not implemented");
  }

}
