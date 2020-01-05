package net.bytle.api;

import io.vertx.core.VertxOptions;

/**
 * The Vertx Launcher
 * https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher
 * By default, it executed the run command {@link }
 */
public class Launcher extends io.vertx.core.Launcher {



  @Override
  public void beforeStartingVertx(VertxOptions options) {

    options.setMetricsOptions(DropWizard.getMetricsOptions());

  }

  public static void main(String[] args) {

    new Launcher().dispatch(args);

  }

}
