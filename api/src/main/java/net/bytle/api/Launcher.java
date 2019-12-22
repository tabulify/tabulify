package net.bytle.api;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;

/**
 * The Vertx Launcher
 * https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher
 * By default, it executed the run command {@link }
 */
public class Launcher extends io.vertx.core.Launcher {


  private ConfigRetriever configRetriever;

  @Override
  public void beforeStartingVertx(VertxOptions options) {

    options.setMetricsOptions(DropWizard.getMetricsOptions());

  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {



  }



  public static void main(String[] args) {

    new Launcher().dispatch(args);

  }
}
