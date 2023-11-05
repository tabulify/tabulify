package net.bytle.tower;


import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.BrowserSessionHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.schedule.SqlAnalytics;
import net.bytle.tower.util.DatacadamiaDomain;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.GlobalUtilityObjectsCreation;
import net.bytle.tower.util.PersistentLocalSessionStore;
import net.bytle.vertx.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * <a href="https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients">Doc</a>
 */
public class VerticleApi extends AbstractVerticle {

  static {
    Log4JManager.setConfigurationProperties();
  }

  protected static final Logger LOGGER = LogManager.getLogger(VerticleApi.class);

  public static final int PORT_DEFAULT = 8083;
  private EraldyApiApp apiApp;

  public static void main(String[] args) {

    new MainLauncher().dispatch(new String[]{"run", VerticleApi.class.getName()});

  }


  @Override
  public void start(Promise<Void> verticlePromise) {

    LOGGER.info("Api Verticle Started");
    ConfigManager.config("tower", this.vertx, this.config())
      .build()
      .getConfigAccessor()
      .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
      .onSuccess(configAccessor -> vertx.executeBlocking(() -> {
          GlobalUtilityObjectsCreation
            .create(vertx, configAccessor)
            .init();
          return null;
        })
        .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
        .onSuccess(Void -> {

          /**
           * Create the server
           */
          Server server;
          try {
            server = Server.create("http", vertx, configAccessor)
              .setFromConfigAccessorWithPort(PORT_DEFAULT)
              .enableApiKeyAuth()
              .enableJwt()
              .enableHashId()
              .enableJdbcPool("jdbc")
              .build();
          } catch (IllegalConfiguration e) {
            this.handlePromiseFailure(verticlePromise,e);
            return;
          }

          /**
           * Create the HTTP server
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

          /**
           * Add the apps
           */
          apiApp = EraldyApiApp.create(eraldyDomain);
          Future<Void> publicApiFuture = apiApp.mount();

          /**
           * Domain Handler
           */
          Router router = httpServer.getRouter();
          AuthRealmHandler.createFrom(router, apiApp);
          BrowserCorsUtil.allowCorsForApexDomain(router, eraldyDomain); // Allow Browser cross-origin request in the domain
          BrowserSessionHandler.addBrowserSessionHandler(router, eraldyDomain); // Add the session handler cross domain, cross realm


          Future<Realm> eraldyRealm = EraldyRealm.create(apiApp).getFutureRealm();
          /**
           * Add the scheduled task
           */
          SqlAnalytics.create(eraldyDomain);

          List<Future<?>> initFutures = Lists.newArrayList(publicApiFuture, eraldyRealm);

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

          Future.join(initFutures)
            .onFailure(FailureStatic::failFutureWithTrace)
            .onSuccess(apiFutureResult -> httpServer.buildHttpServer()

              /**
               * https://vertx.io/docs/vertx-core/java/#_handling_requests
               */
              .requestHandler(router)
              .listen(ar -> {
                if (ar.succeeded()) {
                  LOGGER.info("API HTTP server running on port " + ar.result().actualPort());
                  verticlePromise.complete();
                } else {
                  LOGGER.error("Could not start the API HTTP server " + ar.cause());
                  this.handlePromiseFailure(verticlePromise, ar.cause());
                }
              }));

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
      return null;

    });

  }


  public EraldyApiApp getApp() {
    return apiApp;
  }

}
