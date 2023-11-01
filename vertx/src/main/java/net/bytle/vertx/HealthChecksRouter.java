package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health Check code,
 * not really well known, what is does
 */
public class HealthChecksRouter {

  protected static final Logger LOGGER = LoggerFactory.getLogger(HealthChecksRouter.class);

  public static void addHealtChecksToRouter(Router router, Vertx vertx, HttpServer.builder httpServerBuilder) {


    String defaultPokemonPath = "v2/pokemon";
    HealthHandlerCircuitBreaker handlerPokemon = new HealthHandlerCircuitBreaker(vertx)
      .setPokeApiUrl(
        httpServerBuilder.server.getListeningHost(),
        httpServerBuilder.server.getListeningPort(),
        defaultPokemonPath
      );

    vertx
      .eventBus().<JsonObject>consumer(
        EventBusChannels.CONFIGURATION_CHANGED.name(),
        message -> {
          LOGGER.debug("Configuration has changed, updating...");
          JsonObject newConfiguration = message.body();
          handlerPokemon.setPokeApiUrl(
            newConfiguration.getString(Server.HOST, "localhost"),
            newConfiguration.getInteger(Server.LISTENING_PORT,  httpServerBuilder.server.getListeningPort()),
            defaultPokemonPath
          );
          LOGGER.debug("Configuration has changed, has been updated...");
        });

    // Healthy services ?
    // You can register several check (ie for db, for ...)
    router.get("/alive").handler(
      HealthCheckHandler
        .create(vertx)
        .register("ok", promiseOk -> {
          JsonObject content = new JsonObject().put("ok", "ok");
          promiseOk.complete(Status.OK(content));
        })
    );
    router.get("/healthy").handler(
      HealthCheckHandler
        .createWithHealthChecks(handlerPokemon.getHealthChecks()));

  }
}
