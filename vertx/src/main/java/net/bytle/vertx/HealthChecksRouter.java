package net.bytle.vertx;

import io.vertx.core.AbstractVerticle;
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

  public static void addHealtChecksToRouter(Router router, AbstractVerticle verticle, HttpServer.builder httpServerBuilder) {


    String defaultPokemonPath = "v2/pokemon";
    HealthHandlerCircuitBreaker handlerPokemon = new HealthHandlerCircuitBreaker(verticle.getVertx())
      .setPokeApiUrl(
        httpServerBuilder.serverProperties.getListeningHost(),
        httpServerBuilder.serverProperties.getListeningPort(),
        defaultPokemonPath
      );

    verticle.getVertx()
      .eventBus().<JsonObject>consumer(
        EventBusChannels.CONFIGURATION_CHANGED.name(),
        message -> {
          LOGGER.debug("Configuration has changed, verticle {} is updating...", verticle.deploymentID());
          JsonObject newConfiguration = message.body();
          handlerPokemon.setPokeApiUrl(
            newConfiguration.getString(ServerProperties.HOST, "localhost"),
            newConfiguration.getInteger(ServerProperties.LISTENING_PORT,  httpServerBuilder.serverProperties.getListeningPort()),
            defaultPokemonPath
          );
          LOGGER.debug("Configuration has changed, verticle {} has been updated...", verticle.deploymentID());
        });

    // Healthy services ?
    // You can register several check (ie for db, for ...)
    router.get("/alive").handler(
      HealthCheckHandler
        .create(verticle.getVertx())
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
