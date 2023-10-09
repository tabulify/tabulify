package net.bytle.edge;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import net.bytle.edge.routehandler.DmarcHandler;
import net.bytle.edge.routehandler.StatusHandler;
import net.bytle.s3.AwsBucket;
import net.bytle.vertx.ConfigManager;
import net.bytle.vertx.ServerConfig;
import net.bytle.vertx.ServerStartLogger;
import org.apache.logging.log4j.Logger;


public class EdgeVerticle extends AbstractVerticle {

  public static final int PORT = 8084;
  private static final Logger LOGGER = ServerStartLogger.START_LOGGER;

  @Override
  public void start(Promise<Void> verticlePromise) {


    LOGGER.info("Edge Verticle Started");
    ConfigManager.config("edge", this.vertx, this.config())
      .build()
      .getConfigAccessor()
      .onFailure(verticlePromise::fail)
      .onSuccess(configAccessor -> vertx.executeBlocking(EdgeConfig.create(this, configAccessor))
        .onFailure(verticlePromise::fail)
        .onSuccess(Void -> {

          /**
           * Deploy HTTP verticle
           * <p>
           * Check binding
           * https://fly.io/docs/getting-started/app-services/#a-note-on-ipv4-and-ipv6-wildcards
           */
          ServerConfig serverConfig = ServerConfig.create(configAccessor);
          int listeningPortNumber = serverConfig.getListeningPort(PORT);
          String listeningHostName = serverConfig.getListeningHost();
          HttpServerOptions options = new HttpServerOptions()
            .setLogActivity(true)
            .setHost(listeningHostName)
            .setPort(listeningPortNumber);


          Router rootRouter = Router.router(vertx);

          rootRouter.route().handler(BodyHandler.create());

          rootRouter.get("/status")
            .handler(StatusHandler.create());
          rootRouter.post(DmarcHandler.DMARC_PATH)
            .handler(DmarcHandler.create());

          vertx.createHttpServer(options)

            /**
             * https://vertx.io/docs/vertx-core/java/#_handling_requests
             */
            .requestHandler(rootRouter)
            .listen(ar -> {
              if (ar.succeeded()) {
                LOGGER.info("Edge HTTP server running on port " + listeningPortNumber);
                /**
                 * Connection check at the end
                 * to have a quick cold start
                 * when the vm is stopped and start
                 * because of incoming call
                 */
                vertx.executeBlocking(() -> {
                  AwsBucket.get().checkConnection()
                    .onFailure(verticlePromise::fail)
                    .onSuccess(v -> verticlePromise.complete());
                  return null;
                });
              } else {
                LOGGER.error("Could not start a HTTP server on port (" + listeningPortNumber + ") " + ar.cause());
                verticlePromise.fail(ar.cause());
              }
            });
        }));

  }


}
