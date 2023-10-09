package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.AppPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.App;

public class AppPublicapiImpl implements AppPublicapi {
  @Override
  public Future<ApiResponse<App>> appGet(RoutingContext routingContext) {
    return null;
  }
}
