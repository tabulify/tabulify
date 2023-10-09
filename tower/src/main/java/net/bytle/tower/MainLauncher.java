package net.bytle.tower;

import io.vertx.core.VertxOptions;
import net.bytle.tower.util.DropWizard;
import net.bytle.tower.util.Log4JManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A custom <a href="https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher">Vertx Launcher</a>
 * (to set environment before launching)
 * <p></p>
 * The vert.x  Launcher is used in:
 * * fat jar as main class,
 * * and by the vertx command line utility.
 *
 */
public class MainLauncher extends io.vertx.core.Launcher {

  static {
    Log4JManager.setConfigurationProperties();
  }

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    LOGGER.info("Setting DropWizard Metrics");
    options.setMetricsOptions(DropWizard.getMetricsOptions());
  }


  /**
   * The arguments are taken from the fat jar
   * @param args - the arguments to dispatch
   */
  public static void main(String[] args) {

    new MainLauncher().dispatch(args);

  }


}
