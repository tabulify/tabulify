package net.bytle.tower.eraldy.api.implementer.flow;

import net.bytle.tower.eraldy.api.implementer.callback.UserLoginEmailCallback;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.flow.WebFlowAbs;

public class EmailLoginFlow extends WebFlowAbs {
  private final UserLoginEmailCallback callback;

  public EmailLoginFlow(TowerApp towerApp) {
    super(towerApp);
    this.callback = new UserLoginEmailCallback(this);
  }

  public UserLoginEmailCallback getCallback() {
    return this.callback;
  }
}
