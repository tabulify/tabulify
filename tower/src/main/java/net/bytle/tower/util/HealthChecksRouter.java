package net.bytle.tower.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import net.bytle.vertx.EventBusChannels;
import net.bytle.vertx.HealthHandlerCircuitBreaker;
import net.bytle.vertx.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytle.tower.VerticleApi.PORT_DEFAULT;

/**
 * Health Check code,
 * not really well known, what is does
 */
public class HealthChecksRouter {

  protected static final Logger LOGGER = LoggerFactory.getLogger(HealthChecksRouter.class);

  public static void addHealtChecksToRouter(Router router, AbstractVerticle verticle) {


    String defaultPokemonPath = "v2/pokemon";
    JsonObject config = verticle.config();
    HealthHandlerCircuitBreaker handlerPokemon = new HealthHandlerCircuitBreaker(verticle.getVertx())
      .setPokeApiUrl(
        config.getString(ServerProperties.HOST.toString(), "localhost"),
        config.getInteger(ServerProperties.PORT.toString(), PORT_DEFAULT),
        config.getString(ServerProperties.PATH.toString(), defaultPokemonPath)
      );

    verticle.getVertx()
      .eventBus().<JsonObject>consumer(
        EventBusChannels.CONFIGURATION_CHANGED.name(),
        message -> {
          LOGGER.debug("Configuration has changed, verticle {} is updating...", verticle.deploymentID());
          JsonObject newConfiguration = message.body();
          handlerPokemon.setPokeApiUrl(
            newConfiguration.getString(ServerProperties.HOST.toString(), "localhost"),
            newConfiguration.getInteger(ServerProperties.PORT.toString(), PORT_DEFAULT),
            newConfiguration.getString(ServerProperties.PATH.toString(), defaultPokemonPath)
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
    router.get("/healthy").handler(HealthCheckHandler.createWithHealthChecks(handlerPokemon.getHealthChecks()));

  }
}
