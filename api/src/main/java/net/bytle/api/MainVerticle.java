package net.bytle.api;


import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.bytle.api.db.DatabaseVerticle;
import net.bytle.api.http.VerticleHttpServer;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> promise) {

    // Create a config retriever
    ConfigRetriever retriever = Conf.getConfigRetriever(vertx);


    retriever.getConfig(ar -> {
      if (ar.failed()) {

        promise.fail(ar.cause());

      } else {

        JsonObject config = ar.result();

        // Start the verticle from the config
        int instances = 1; // May be Runtime.getRuntime().availableProcessors();
        DeploymentOptions deploymentOptions = new DeploymentOptions()
          .setInstances(instances)
          .setConfig(config);

        // Deploy database verticle
        Promise<String> dbCompletionHandler = Promise.promise();
        vertx.deployVerticle(new DatabaseVerticle(), deploymentOptions, dbCompletionHandler);

        // On completion, deploy the http web server
        // Note id is the deployment id
        dbCompletionHandler.future().compose(
          id -> {

            Promise<String> httpVerticleDeployment = Promise.promise();
            vertx.deployVerticle(
              new VerticleHttpServer(),
              deploymentOptions,
              httpVerticleDeployment);

            return httpVerticleDeployment.future();

          }).setHandler(arDbCompletion -> {
          if (arDbCompletion.succeeded()) {
            promise.complete();
          } else {
            promise.fail(arDbCompletion.cause());
          }
        });
      }
    });

  }
}


