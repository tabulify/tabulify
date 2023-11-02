package net.bytle.ip;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.vertx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpVerticle extends AbstractVerticle {



  static {
    Log4JManager.setConfigurationProperties();
  }

  protected static final Logger LOGGER = LoggerFactory.getLogger(IpVerticle.class);

  public static final int PORT_DEFAULT = 8084;

  public static void main(String[] args) {

    new MainLauncher().dispatch(new String[]{"run", IpVerticle.class.getName()});

  }

  @Override
  public void start(Promise<Void> verticlePromise) {
    LOGGER.info("IP Verticle Started");
    ConfigManager.config("ip", this.vertx, this.config())
      .build()
      .getConfigAccessor()
      .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
      .onSuccess(configAccessor -> {

        // The server
        vertx.executeBlocking(() -> Server
            .create("http", vertx, configAccessor)
            .setFromConfigAccessorWithPort(PORT_DEFAULT)
            .addJdbcPool("pg") // postgres
            .addIpGeolocation() // ip geolocation
            .build()
          ).onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
          .onSuccess(server -> {
            /**
             * Create the base router with the base Handler
             */
            HttpServer httpServer;
            try {
              httpServer = HttpServer.createFromServer(server)
                .addBodyHandler() // body transformation
                .addWebLog() // web log
                .setBehindProxy() // enable proxy forward
                .enableFailureHandler() // enable failure handler
                .addFakeErrorHandler()
                .addHealthCheck()
                .build();
            } catch (IllegalConfiguration e) {
              this.handlePromiseFailure(verticlePromise, e);
              return;
            }

            EraldyDomain eraldyDomain = EraldyDomain.getOrCreate(httpServer, configAccessor);
            vertx.executeBlocking(() -> IpApp.createForDomain(eraldyDomain).mount())
              .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
              .onSuccess(Void -> httpServer.buildHttpServer()
                .requestHandler(httpServer.getRouter())
                .listen(ar -> {
                  if (ar.succeeded()) {
                    LOGGER.info("HTTP server running on port " + ar.result().actualPort());
                    verticlePromise.complete();
                  } else {
                    LOGGER.error("Could not start the HTTP server " + ar.cause());
                    this.handlePromiseFailure(verticlePromise, ar.cause());
                  }
                }));
          });


      });


  }

  private void handlePromiseFailure(Promise<Void> promise, Throwable e) {
    promise.fail(e);
    this.vertx.close();
  }

}
