package net.bytle.api;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

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
    // Start the verticle from the config
    int instances = 1; // May be Runtime.getRuntime().availableProcessors();
    deploymentOptions
      .setInstances(instances);

    Future<JsonObject> future = Future.future(configRetriever::getConfig);
    future.setHandler(ar -> {
      if (ar.failed()) {
        // Failed to retrieve the configuration
      } else {
        deploymentOptions.setConfig(ar.result());
      }
    });


  }

  @Override
  public void afterStartingVertx(Vertx vertx) {
    configRetriever = Conf.getConfigRetriever(vertx);
  }

  private JsonObject addHttpConfig(JsonObject jsonObject) {
    return jsonObject.mergeIn(new JsonObject()
      .put(Conf.Properties.HOST.toString(), "localhost")
      .put(Conf.Properties.PORT.toString(), 8080)
      .put(Conf.Properties.PATH.toString(), "v2/pokemon")
    );
  }


  public static void main(String[] args) {

    new Launcher().dispatch(args);

  }
}
