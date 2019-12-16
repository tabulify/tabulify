package net.bytle.api.http;


import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class HandlerPokemon implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(HandlerPokemon.class);
  private static final JsonArray FALLBACK = new JsonArray();

  private final WebClient webClient;

  /**
   * Vert.x Circuit Breaker is an implementation of the Circuit Breaker pattern for Vert.x.
   * It keeps track of the number of failures and opens the circuit when a threshold is reached. Optionally, a fallback is executed.
   * https://vertx.io/docs/vertx-circuit-breaker/java/
   */
  private final CircuitBreaker circuitBreaker;
  private final HealthChecks healthChecks;

  private int pokeApiPort;
  private String pokeApiHost;
  private String pokeApiPath;

  public HandlerPokemon(Vertx vertx) {

    this.webClient = WebClient.create(vertx,
      new WebClientOptions()
        .setKeepAlive(true)
        .setSsl(true)
    );

    CircuitBreakerOptions circuitBreakerOptions = new CircuitBreakerOptions()
      .setMaxFailures(3)
      .setTimeout(1000)
      .setFallbackOnFailure(true)
      .setResetTimeout(60000);

    this.circuitBreaker = CircuitBreaker.create("pokeapi", vertx, circuitBreakerOptions);
    this.circuitBreaker.openHandler(v -> logger.info("{} circuit breaker is open", "pokeapi"));
    this.circuitBreaker.closeHandler(v -> logger.info("{} circuit breaker is closed", "pokeapi"));
    this.circuitBreaker.halfOpenHandler(v -> logger.info("{} circuit breaker is half open", "pokeapi"));

    this.healthChecks = HealthChecks.create(vertx);
    healthChecks.register("pokeApiHealthcheck", 1000, future -> {
      if (circuitBreaker.state().equals(CircuitBreakerState.CLOSED)) {
        future.complete(Status.OK());
      } else {
        future.complete(Status.KO());
      }
    });

  }

  @Override
  public void handle(RoutingContext context) {

    Function<Throwable, JsonArray> fallback = future -> FALLBACK;

    Handler<Promise<JsonArray>> processor = Promise -> {
      webClient.get(pokeApiPort, pokeApiHost, pokeApiPath).send(result -> {
        if (result.succeeded()) {
          Promise.complete(result.result().bodyAsJsonObject().getJsonArray("results"));
        } else {
          Promise.fail(result.cause());
        }
      });
    };

    Handler<AsyncResult<JsonArray>> callback = result -> {
      if (result.succeeded()) {
        JsonArray pokemons = result.result();
        context.response().setStatusCode(200).end(Json.encodePrettily(pokemons));
      } else {
        Throwable cause = result.cause();
        logger.error(cause.getMessage(), cause);
        context.response().setStatusCode(500).end(cause.getMessage());
      }
    };

    circuitBreaker.executeWithFallback(processor, fallback).setHandler(callback);
  }

  public HandlerPokemon setPokeApiUrl(String pokeApiHost, int pokeApiPort, String pokeApiPath) {
    this.pokeApiHost = pokeApiHost;
    this.pokeApiPort = pokeApiPort;
    this.pokeApiPath = pokeApiPath;
    return this;
  }

  public HealthChecks getHealthchecks() {
    return healthChecks;
  }
}
