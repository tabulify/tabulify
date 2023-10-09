package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.ListPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.RegistrationList;

import java.util.List;

public class ListPublicapiImpl implements ListPublicapi {



  @Override
  public Future<ApiResponse<List<RegistrationList>>> listsGet(RoutingContext routingContext) {
    return null;
  }
}
