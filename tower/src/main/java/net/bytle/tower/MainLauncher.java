package net.bytle.tower;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import net.bytle.vertx.Log4JManager;
import net.bytle.vertx.VertxPrometheusMetrics;

/**
 * A custom <a href="https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher">Vertx Launcher</a>
 * (to set environment before launching)
 * <p></p>
 * The vert.x  Launcher is used in:
 * * fat jar as main class,
 * * and by the vertx command line utility.
 * There is also a cli API
 * <a href="https://vertx.io/docs/vertx-core/java/#_vert_x_command_line_interface_api">CLI</a>
 */
public class MainLauncher extends io.vertx.core.Launcher {

  static {
    Log4JManager.setConfigurationProperties();
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(options);
    options.setMetricsOptions(VertxPrometheusMetrics.getInitMetricsOptions());
  }

  @Override
  public void afterStartingVertx(Vertx vertx) {
    super.afterStartingVertx(vertx);
    VertxPrometheusMetrics.configEnableHistogramBuckets();
  }

  /**
   * The arguments are taken from the fat jar
   * @param args - the arguments to dispatch
   */
  public static void main(String[] args) {

    new MainLauncher().dispatch(args);

  }


}
