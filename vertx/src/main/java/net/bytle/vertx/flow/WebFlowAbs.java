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
    this.towerApp = towerApp;
    this.towerApp.getHttpServer().getServer().registerService(this);
  }

  @Override
  public TowerApp getApp() {
    return this.towerApp;
  }

}
