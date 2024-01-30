package net.bytle.tower;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.util.GlobalUtilityObjectsCreation;
import net.bytle.vertx.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

            /**
             * Create the server
             */
            Server server;
            server = Server.create("http", vertx, configAccessor)
              .setFromConfigAccessorWithPort(PORT_DEFAULT)
              .enableApiKeyAuth()
              .enableJwt()
              .enableHashId()
              .enablePostgresDatabase()
              .enableJsonToken()
              .enableSmtpClient()
              .enableMapDb()
              .enableTrackerAnalytics()
              .enableDnsClient()
              .build();

            /**
             * Create the HTTP server
             */
            HttpServer httpServer;
            httpServer = HttpServer.createFromServer(server)
              .addBodyHandler() // body transformation
              .addWebLog() // web log
              .setBehindProxy() // enable proxy forward
              .enableFailureHandler() // enable failure handler
              .addFakeErrorHandler()
              .enablePersistentSessionStore()
              .build();

            /**
             * App
             */
            EraldyDomain eraldyDomain = EraldyDomain.getOrCreate(httpServer, configAccessor);
            apiApp = EraldyApiApp.create(eraldyDomain);

            /**
             * Future to be executed before the HTTP server listen
             */
            Future<Void> publicApiFuture = apiApp.mount();
            httpServer.addFutureToExecuteOnBuild(publicApiFuture);

            /**
             * Create the server
             * https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients
             *  0.0.0.0 means listen on all available addresses
             */
            return httpServer.buildVertxHttpServer();
          })
          .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
          .onSuccess(httpServerFuture -> httpServerFuture
            .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
            .onSuccess(vertxHttpServer -> vertxHttpServer
              .listen(ar -> {
                if (ar.succeeded()) {
                  LOGGER.info("API HTTP server running on port " + ar.result().actualPort());
                  verticlePromise.complete();
                } else {
                  LOGGER.error("Could not start the API HTTP server " + ar.cause());
                  this.handlePromiseFailure(verticlePromise, ar.cause());
                }
              })))
      );


  }


  private void handlePromiseFailure(Promise<Void> promise, Throwable e) {
    promise.fail(e);
    // advertise the failure otherwise the error is not seen
    LOGGER.error("Unable to start the verticle Api", e);
    // closing vertx may create additional error if there is Futures running
    // This is the only way known to stop the process
    // use Future.join in the building/start process of the app is one solution
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

      LOGGER.info("Closing Server services");
      this.getApp().getApexDomain().getHttpServer().close(); // close also server

      stopPromise.complete();
      return null;

    });

  }


  public EraldyApiApp getApp() {
    return apiApp;
  }

}
