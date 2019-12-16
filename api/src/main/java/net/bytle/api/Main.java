package net.bytle.api;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import net.bytle.api.db.DatabaseVerticle;
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
    future.setHandler(configAsyncResult -> {
      if (configAsyncResult.failed()) {
        // Failed to retrieve the configuration
        System.out.println(configAsyncResult.cause());
      } else {
        deploymentOptions.setConfig(configAsyncResult.result());

        Promise<String> dbCompletionHandler = Promise.promise();
        vertx.deployVerticle(new DatabaseVerticle(), deploymentOptions, dbCompletionHandler);

        dbCompletionHandler.future().compose(
          id -> {

            Promise<String> httpCompletionHandler = Promise.promise();
            vertx.deployVerticle(
              new VerticleHttpServer(),
              deploymentOptions,
              httpCompletionHandler);

            return httpCompletionHandler.future();

          }).setHandler(ar -> {
          if (ar.succeeded()) {
            // success
          } else {
            throw new RuntimeException("Unable to start the web server",ar.cause());
          }
        });
      }
    });


  }

}
