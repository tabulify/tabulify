package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import net.bytle.vertx.VertxPrometheusMetrics;

public class Https {


  public static Vertx getVertx() {
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMetricsOptions(VertxPrometheusMetrics.getInitMetricsOptions());
    return Vertx.vertx(vertxOptions);
  }

}
