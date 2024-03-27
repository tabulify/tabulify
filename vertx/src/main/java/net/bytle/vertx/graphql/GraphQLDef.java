package net.bytle.vertx.graphql;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.bytle.vertx.TowerApp;

public interface GraphQLDef {
  TowerApp getApp();


  Handler<RoutingContext> getHandler();

}
