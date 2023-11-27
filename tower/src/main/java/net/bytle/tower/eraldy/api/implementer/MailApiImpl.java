package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.interfaces.MailApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.vertx.TowerApp;

public class MailApiImpl implements MailApi {
  @SuppressWarnings("unused")
  public MailApiImpl(TowerApp towerApp) {
    //
  }

  @Override
  public Future<ApiResponse<Void>> mailEmailEmailValidationGet(RoutingContext routingContext, String email) {

    System.out.println(email);
    return Future.succeededFuture(new ApiResponse<>());

  }
}
