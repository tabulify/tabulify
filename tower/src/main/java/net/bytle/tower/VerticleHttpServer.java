package net.bytle.tower;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.tower.eraldy.app.ErrorFakeHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.util.DatacadamiaDomain;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.HealthChecksEventBus;
import net.bytle.tower.util.HealthChecksRouter;
import net.bytle.vertx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <a href="https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients">Doc</a>
 */
public class VerticleHttpServer extends AbstractVerticle {


  protected static final Logger LOGGER = LoggerFactory.getLogger(VerticleHttpServer.class);

  public static final int PORT_DEFAULT = 8083;




  @Override
  public void start(Promise<Void> promise) {


    /**
     * Create the base router with the base Handler
     */
    Router rootRouter = RootRouterBuilder.create(this)
      .addBodyHandler() // body transformation
      .addWebLog() // web log
      .getRouter();

    /**
     * Health check things
     */
    HealthChecksRouter.addHealtChecksToRouter(rootRouter, this);
    HealthChecksEventBus.registerHandlerToEventBus(vertx);

    /**
     * Forward proxy is disabled by default
     */
    HttpForwardProxy.addAllowForwardProxy(rootRouter);


    /**
     * Failure Handler / Route match failures
     * https://vertx.io/docs/vertx-web/java/#_route_match_failures
     */
    VertxRoutingFailureHandler errorHandlerXXX = VertxRoutingFailureHandler.createOrGet(vertx, config());
    rootRouter.errorHandler(HttpStatus.INTERNAL_ERROR, errorHandlerXXX);

    /**
     * Handle the failures. ie
     * ```
     * ctx.fail(400, error)
     * ```
     */
    rootRouter.route().failureHandler(errorHandlerXXX);

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
          .requestHandler(rootRouter)
          .listen(ar -> {
            if (ar.succeeded()) {
              LOGGER.info("HTTP server running on port " + listeningPortNumber);
              promise.complete();
            } else {
              LOGGER.error("Could not start a HTTP server on port (" + listeningPortNumber + ") " + ar.cause());
              promise.fail(ar.cause());
            }
          });
      });


  }


}
