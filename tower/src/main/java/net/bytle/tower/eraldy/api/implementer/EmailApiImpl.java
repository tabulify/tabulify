package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.interfaces.EmailApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.vertx.TowerApp;

public class EmailApiImpl implements EmailApi {
  @SuppressWarnings("unused")
  public EmailApiImpl(TowerApp towerApp) {
    //
  }

  @Override
  public Future<ApiResponse<Void>> emailAddressAddressValidationGet(RoutingContext routingContext, String email) {

    System.out.println(email);
    return Future.succeededFuture(new ApiResponse<>());

  }
}
