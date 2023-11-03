package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.openapi.interfaces.CampaignApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Campaign;
import net.bytle.tower.eraldy.model.openapi.CampaignEmailPostBody;
import net.bytle.vertx.TowerApp;

public class CampaignApiImpl implements CampaignApi {

  public CampaignApiImpl(TowerApp towerApp) {

  }

  @Override
  public Future<ApiResponse<Campaign>> campaignEmailPost(RoutingContext routingContext, CampaignEmailPostBody campaignEmailPostBody) {
    throw new InternalException("Not implemented");
  }

}
