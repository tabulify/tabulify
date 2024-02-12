package net.bytle.vertx.flow;

import net.bytle.vertx.TowerApp;

public interface WebFlow {


  TowerApp getApp();

  /**
   * A unique id/type that identifies the flow
   * for analytics purpose
   * To be sure that they are unique, we keep
   * them in {@link FlowType}
   */
  FlowType getFlowType();

}
