package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.RealmPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Realm;

public class RealmPublicapiImpl implements RealmPublicapi {
  @Override
  public Future<ApiResponse<Realm>> realmGet(RoutingContext routingContext) {
    return null;
  }
}
