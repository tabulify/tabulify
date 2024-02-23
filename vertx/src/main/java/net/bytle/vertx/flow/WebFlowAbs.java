package net.bytle.vertx.flow;

import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerService;

/**
 * Represents a web flow, they are {@link TowerService}
 * because they generally register callback at mount time
 */
public abstract class WebFlowAbs extends TowerService implements WebFlow {

  private final TowerApp towerApp;

  public WebFlowAbs(TowerApp towerApp) {
    super(towerApp.getServer());
    this.towerApp = towerApp;
  }

  @Override
  public TowerApp getApp() {
    return this.towerApp;
  }

}
