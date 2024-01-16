package net.bytle.tower;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.AuthSessionHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.util.DatacadamiaDomain;
import net.bytle.tower.util.Env;
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
              .enableJsonToken()
              .enableSmtpClient("Eraldy.com")
              .enableMapDb()
              .enableTrackerAnalytics()
              .enableDnsClient()
              .build();
          } catch (ConfigIllegalException e) {
            this.handlePromiseFailure(verticlePromise, e);
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
              .enablePersistentSessionStore()
              .build();
          } catch (IllegalConfiguration e) {
            this.handlePromiseFailure(verticlePromise, e);
            return;
          }

          /**
           * App
           */
          EraldyDomain eraldyDomain = EraldyDomain.getOrCreate(httpServer, configAccessor);
          try {
            apiApp = EraldyApiApp.create(eraldyDomain);
          } catch (ConfigIllegalException e) {
            this.handlePromiseFailure(verticlePromise, e);
            return;
          }

          /**
           * Building Router
           */
          Router router = httpServer.getRouter();
          AuthRealmHandler.createFrom(router, apiApp);
          AuthSessionHandler.addAuthCookieSessionHandler(router, apiApp); // Add the session handler cross domain, cross realm
          BrowserCorsUtil.allowCorsForApexDomain(router, eraldyDomain); // Allow Browser cross-origin request in the domain

          /**
           * Future to be executed before the HTTP server listen
           */
          Future<Void> publicApiFuture = apiApp.mount();
          httpServer.addFutureToExecuteOnBuild(publicApiFuture);
          Future<Realm> eraldyRealm = EraldyRealm.create(apiApp).getFutureRealm(); // Eraldy creation
          httpServer.addFutureToExecuteOnBuild(eraldyRealm);
          if (Env.IS_DEV) {
            // Add the realm for datacadamia for test/purpose only
            Future<Realm> realm = DatacadamiaDomain.getOrCreate(this).createRealm();
            httpServer.addFutureToExecuteOnBuild(realm);
          }

          /**
           * Create the server
           * https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients
           *  0.0.0.0 means listen on all available addresses
           */
          httpServer
            .buildHttpServer()
            .onFailure(err -> this.handlePromiseFailure(verticlePromise, err))
            .onSuccess(vertxHttpServer -> vertxHttpServer
              .requestHandler(router) // https://vertx.io/docs/vertx-core/java/#_handling_requests
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
