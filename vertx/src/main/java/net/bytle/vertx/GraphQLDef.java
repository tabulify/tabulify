package net.bytle.vertx;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface GraphQLDef {
  TowerApp getApp();


  Handler<RoutingContext> getHandler();

}
