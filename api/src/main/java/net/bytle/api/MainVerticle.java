package net.bytle.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.bytle.api.http.HttpServerVerticle;

import static net.bytle.api.http.HttpServerVerticle.CONFIG_HTTP_SERVER_PORT;

public class MainVerticle extends AbstractVerticle {


  @Override
  public void start(Promise<Void> promise) throws Exception {


    Promise<String> deployHttpPromise = Promise.promise();

    JsonObject jsonObject = new JsonObject()
      .put(CONFIG_HTTP_SERVER_PORT, 8083);

    vertx.deployVerticle(
      new HttpServerVerticle(),
      new DeploymentOptions().setConfig(jsonObject),
      deployHttpPromise);

    deployHttpPromise.future().setHandler(ar -> {
      if (ar.succeeded()) {
        promise.complete();
      } else {
        promise.fail(ar.cause());
      }
    });
  }

}
