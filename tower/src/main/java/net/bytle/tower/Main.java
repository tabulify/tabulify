package net.bytle.tower;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import net.bytle.vertx.Log4JManager;
import net.bytle.vertx.VertxPrometheusMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

  static {
    Log4JManager.setConfigurationProperties();
  }

  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * A main that deploys the {@link MainVerticle} (ie start)
   * <p>
   * We use for now {@link MainLauncher#main(String[])}
   * <p>
   * Be careful that you need to change the working directory
   * to the module (test are running in the module but
   * main are running in the root of the project)
   */
  public static void main(String[] args) {

    VertxOptions vertxOptions = new VertxOptions()
      .setMetricsOptions(VertxPrometheusMetrics.getInitMetricsOptions());
    Vertx vertx = Vertx.vertx(vertxOptions);
    vertx.deployVerticle(new MainVerticle())
      .onFailure(e -> {
        LOGGER.error("Be careful that you need to change the working directory\n" +
          "   * to the tower module\n" +
          "(test are running in the module but main are running in the root of the project)");
        e.printStackTrace();
        System.exit(1);
      })
      .onSuccess(s -> LOGGER.info("Main verticle started"));

  }

}
