package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class Https {


  public static Vertx getVertx() {
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMetricsOptions(DropWizard.getMetricsOptions());
    return Vertx.vertx(vertxOptions);
  }

}
