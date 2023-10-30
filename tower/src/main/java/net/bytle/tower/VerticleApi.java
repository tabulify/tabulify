package net.bytle.tower;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.tower.eraldy.app.ErrorFakeHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.util.*;
import net.bytle.vertx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <a href="https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients">Doc</a>
 */
public class VerticleApi extends AbstractVerticle {

  static {
    Log4JManager.setConfigurationProperties();
  }

  protected static final Logger LOGGER = LoggerFactory.getLogger(VerticleApi.class);

  public static final int PORT_DEFAULT = 8083;

  public static void main(String[] args) {

    new MainLauncher().dispatch(new String[]{"run", VerticleApi.class.getName()});

  }


  @Override
  public void start(Promise<Void> verticlePromise) {

    LOGGER.info("Api Verticle Started");
    ConfigManager.config("tower", this.vertx, this.config())
      .build()
      .getConfigAccessor()
      .onFailure(verticlePromise::fail)
      .onSuccess(configAccessor -> vertx.executeBlocking(() -> {
          GlobalUtilityObjectsCreation
            .create(vertx, configAccessor)
            .init();
          return null;
        })
        .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
        .onSuccess(Void -> {

          /**
           * Create the base router with the base Handler
           */
          Router rootRouter;
          try {
            rootRouter = RootRouterBuilder.create(this)
              .addBodyHandler() // body transformation
              .addWebLog() // web log
              .setBehindProxy() // enable proxy forward
              .enableFailureHandler() // enable failure handler
              .getRouter();
          } catch (IllegalConfiguration e) {
            this.handlePromiseFailure(verticlePromise, e);
            return;
          }

          /**
           * Health check things
           */
          HealthChecksRouter.addHealtChecksToRouter(rootRouter, this);
          HealthChecksEventBus.registerHandlerToEventBus(vertx);

          /**
           * Produce an error, not only for dev, also for production to live test (mail, ...)
           */
          rootRouter.get(ErrorFakeHandler.URI_PATH).handler(new ErrorFakeHandler());


          /**
           * Create the domain, its realm and its app
           */
          List<Future<?>> initFutures = EraldyDomain.getOrCreate(this)
            .mount(rootRouter);

          if (Env.IS_DEV) {
            /**
             * Add the realm for datacadamia for test/purpose only
             */
            Future<Realm> realm = DatacadamiaDomain.getOrCreate(this).createRealm();
            initFutures.add(realm);
          }

          /**
           * Create the server
           * https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients
           *  0.0.0.0 means listen on all available addresses
           */
          Router finalRootRouter = rootRouter;
          Future.all(initFutures)
            .onFailure(FailureStatic::failFutureWithTrace)
            .onSuccess(apiFutureResult -> {

              ServerConfig serverConfig = ServerConfig.create(ConfigAccessor.get());
              int listeningPortNumber = serverConfig.getListeningPort(PORT_DEFAULT);
              String listeningHostName = serverConfig.getListeningHost();
              HttpServerOptions options = new HttpServerOptions()
                .setLogActivity(true)
                .setHost(listeningHostName)
                .setPort(listeningPortNumber);

              HttpsCertificateUtil.createOrGet()
                .enableServerHttps(options);

              vertx.createHttpServer(options)
                /**
                 * https://vertx.io/docs/vertx-core/java/#_handling_requests
                 */
                .requestHandler(finalRootRouter)
                .listen(ar -> {
                  if (ar.succeeded()) {
                    LOGGER.info("HTTP server running on port " + listeningPortNumber);
                    verticlePromise.complete();
                  } else {
                    LOGGER.error("Could not start a HTTP server on port (" + listeningPortNumber + ") " + ar.cause());
                    this.handlePromiseFailure(verticlePromise, ar.cause());
                  }
                });
            });

        }));


  }

  private void handlePromiseFailure(Promise<Void> promise, Throwable e) {
    promise.fail(e);
    this.vertx.close();
  }


  /**
   * This stop runs when we send a SIGKill (ie CTRL+C in the terminal, kill on Linux)
   * <p>
   * <a href="https://vertx.io/docs/vertx-core/java/#_asynchronous_verticle_start_and_stop">...</a>
   * <p>
   * IntelliJ's behavior:
   * As of 2023.1 (partially in earlier versions as well), the "Stop" button should work as described:
   * * try performing the graceful shutdown aka SIGTERM on the first press,
   * * hard terminate process (aka SIGKILL) on the second press,
   * This only works in Run mode, not for Debug (since the debugger may be paused
   * at the moment of stop, and resuming it on pressing "Stop Debug" may be unexpected).
   * <a href="https://youtrack.jetbrains.com/issue/RIDER-35566">Ref</a>
   */
  @Override
  public void stop(Promise<Void> stopPromise) {

    /**
     * We use println because the LOGGER does not show always (Why ??)
     */
    String msg = "Main Verticle Stopped";
    LOGGER.info(msg);
    System.out.println(msg + " (ptln)");

    vertx.executeBlocking(() -> {

      System.out.println("Flushing Session Data");
      PersistentLocalSessionStore.get()
        .flush()
        .close();

      stopPromise.complete();
      //stopPromise.fail(); otherwise
      return null;

    });

  }


}
