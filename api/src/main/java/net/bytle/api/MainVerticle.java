
package net.bytle.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import net.bytle.api.db.DatabaseVerticle;
import net.bytle.api.http.VerticleHttpServer;

/**
 * See {@link Main}
 * Not used but just for information
 */

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> promise) {

    Promise<String> dbCompletionHandler = Promise.promise();
    vertx.deployVerticle(new DatabaseVerticle(), dbCompletionHandler);

    dbCompletionHandler.future().compose(
      id -> {

        Promise<String> httpVerticleDeployment = Promise.promise();
        vertx.deployVerticle(
          new VerticleHttpServer(),
          httpVerticleDeployment);

        return httpVerticleDeployment.future();

      }).setHandler(ar -> {   // <7>
      if (ar.succeeded()) {
        promise.complete();
      } else {
        promise.fail(ar.cause());
      }
    });

  }
}

