package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A custom <a href="https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher">Vertx Launcher</a>
 * (to set environment before launching)
 * <p></p>
 * The vert.x  Launcher is used in:
 * * fat jar as main class,
 * * and by the vertx command line utility.
 * There is also a cli API
 * <a href="https://vertx.io/docs/vertx-core/java/#_vert_x_command_line_interface_api">CLI</a>
 * <p>
 * The launcher can be used in a main method.
 * ```java
 * new MainLauncher().dispatch(new String[]{"run", SmtpVerticle.class.getName()});
 * ```
 * Don't call {@link MainLauncher#executeCommand(String, String...)}, otherwise the hooks are not called
 */
public class MainLauncher extends io.vertx.core.Launcher {

  static final Logger LOGGER;

  static {
    Log4JManager.setConfigurationProperties();
    LOGGER = LogManager.getLogger(MainLauncher.class);
  }


  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(options);
    LOGGER.info("Enabling Metrics");
    options.setMetricsOptions(VertxPrometheusMetrics.getInitMetricsOptions());
  }

  @Override
  public void afterStartingVertx(Vertx vertx) {
    super.afterStartingVertx(vertx);
    try {
      LOGGER.info("Enabling Histogram Metrics");
      VertxPrometheusMetrics.configEnableHistogramBuckets();
      LOGGER.info("Enabling Jvm Metrics");
      VertxPrometheusMetrics.configEnableJvm();
    } catch (IllegalConfiguration e) {
      throw new InternalException(e);
    }

  }

  /**
   * The arguments are taken from the fat jar
   *
   * @param args - the arguments to dispatch
   */
  public static void main(String[] args) {

    new MainLauncher().dispatch(args);

  }


}
