package net.bytle.vertx.flow;

import net.bytle.vertx.TowerApp;

public abstract class WebFlowAbs implements WebFlow {

  private final TowerApp towerApp;

  public WebFlowAbs(TowerApp towerApp) {
    this.towerApp = towerApp;
  }

  @Override
  public TowerApp getApp() {
    return this.towerApp;
  }
}
