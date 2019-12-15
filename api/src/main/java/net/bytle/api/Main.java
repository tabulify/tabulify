package net.bytle.api;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import net.bytle.api.http.VerticleHttpServer;

public class Main {

  public static void main(String[] args) {

    DropwizardMetricsOptions metricsOptions = DropWizard.getMetricsOptions();

    Vertx vertx = Vertx.vertx(
      new VertxOptions()
        .setMetricsOptions(metricsOptions)
    );

    ConfigRetriever configRetriever = Conf.getConfigRetriever(vertx);

    // Start the verticle from the config
    int instances = 1; // May be Runtime.getRuntime().availableProcessors();
    DeploymentOptions deploymentOptions = new DeploymentOptions()
      .setInstances(instances);

    Future<JsonObject> future = Future.future(configRetriever::getConfig);
    future.setHandler(ar -> {
      if (ar.failed()) {
        // Failed to retrieve the configuration
        System.out.println(ar.cause());
      } else {
        deploymentOptions.setConfig(ar.result());
        vertx.deployVerticle(new VerticleHttpServer(),deploymentOptions);
      }
    });


  }

}
