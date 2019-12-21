package net.bytle.api.http;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import net.bytle.api.Conf;
import net.bytle.api.EventBusChannels;
import net.bytle.api.db.DatabaseServiceInterface;
import net.bytle.api.db.DatabaseVerticle;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * <a href="https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients">Doc</a>
 */
public class VerticleHttpServer extends AbstractVerticle {


  private static final Logger LOGGER = LoggerFactory.getLogger(VerticleHttpServer.class);


  private PingHandler pingHandler;
  private HandlerFailure handlerFailure;
  private HandlerPokemon handlerPokemon;
  private GreetingHandler greetingHandler;
  private DatabaseServiceInterface dbService;

  @Override
  public void start(Promise<Void> promise) {

    /**
     * Create the db service proxy that will send the event bus
     */
    dbService = DatabaseServiceInterface.createProxy(vertx, DatabaseVerticle.IP_QUEUE_NAME);

    /**
     * Get config
     */
    JsonObject configuration = config();

    /**
     * Handler
     */
    this.pingHandler = new PingHandler()
      .setMessage(configuration.getString(Conf.Properties.PING_RESPONSE.toString()));
    this.handlerFailure = new HandlerFailure();
    this.handlerPokemon = new HandlerPokemon(vertx)
      .setPokeApiUrl(
        configuration.getString(Conf.Properties.HOST.toString()),
        configuration.getInteger(Conf.Properties.PORT.toString()),
        configuration.getString(Conf.Properties.PATH.toString())
      );
    this.greetingHandler = new GreetingHandler();

    /**
     * React to a configuration change event on the event bus
     */
    vertx
      .eventBus()
      .<JsonObject>consumer(
        EventBusChannels.CONFIGURATION_CHANGED.name(),
        message -> {
          LOGGER.debug("Configuration has changed, verticle {} is updating...", deploymentID());
          JsonObject newConfiguration = message.body();
          pingHandler.setMessage(newConfiguration.getString(Conf.Properties.PING_RESPONSE.toString()));
          handlerPokemon.setPokeApiUrl(
            newConfiguration.getString(Conf.Properties.HOST.toString()),
            newConfiguration.getInteger(Conf.Properties.PORT.toString()),
            newConfiguration.getString(Conf.Properties.PATH.toString())
          );
          LOGGER.debug(
            "Configuration has changed, verticle {} has been updated...", deploymentID());
        });

    /**
     * Validate the structure of a request
     * <a href="https://vertx.io/docs/vertx-web-api-contract/java/#_http_requests_validation">Doc</a>
     */
    HTTPRequestValidationHandler greetingValidationHandler = HTTPRequestValidationHandler
      .create()
      .addHeaderParam("Authorization", ParameterType.GENERIC_STRING, true)
      .addHeaderParam("Version", ParameterType.INT, true)
      .addPathParamWithCustomTypeValidator("name", new NameValidator(), false);

    // Config
    int portNumber = configuration.getInteger(Conf.Properties.PORT.toString(), 8083);
    // 0.0.0.0 means listen on all available addresses
    String hostName = configuration.getString(Conf.Properties.HOST.toString(), "0.0.0.0");

    // Create the server
    // https://vertx.io/docs/vertx-core/java/#logging_network_activity
    HttpServerOptions options = new HttpServerOptions()
      .setLogActivity(true)
      .setHost(hostName)
      .setPort(portNumber);
    HttpServer server = vertx.createHttpServer(options);

    // Routing
    Router router = Router.router(vertx);
    router.get("/ip/:ip").handler(this::ip_info);
    router.get("/ip/").handler(this::ip_info);
    router.get("/ip").handler(this::ip_info);

    // Healthy services ?
    router.get("/alive").handler(HealthCheckHandler.create(vertx));
    router.get("/healthy").handler(HealthCheckHandler.createWithHealthChecks(handlerPokemon.getHealthchecks()));

    // Start the server
    server
      .requestHandler(router)
      .listen(ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + portNumber);
          promise.complete();
        } else {
          LOGGER.error("Could not start a HTTP server " + ar.cause());
          promise.fail(ar.cause());
        }
      });

  }


  /**
   * @param context
   * @param page
   * @param expectedKeys
   * @return Example:
   * JsonObject json = context.getBodyAsJson();
   * if (!validateJsonPageDocument(context, json, "key1", "key2")) {
   * return;
   * }
   */
  private boolean validateJsonPageDocument(RoutingContext context, JsonObject page, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(page::containsKey)) {
      LOGGER.error("Bad page creation JSON payload: " + page.encodePrettily() + " from " + context.request().remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request payload").encode());
      return false;
    }
    return true;
  }


  private void ip_info(RoutingContext context) {
    String ip = context.request().getParam("ip");

    context.response().putHeader("Content-Type", "application/json");
    dbService.getIp(ip, ar -> {
      if (ar.succeeded()) {
        // Use the json object
        JsonObject json = ar.result();
        boolean found = json.getBoolean("found");

        if (true) {
          context.response().setStatusCode(200);
          context.response().end(
            new JsonObject()
              .put("success", true)
              .put("ip", json.getString("ip"))
              .put("country2", json.getString("country2"))
              .put("country3", json.getString("country3"))
              .put("country", json.getString("country"))
              .encode());
        } else {
          context.response().setStatusCode(404);
          context.response().end(
            new JsonObject()
              .put("success", false)
              .put("error", "country not found with the ip (" + ip + ")")
              .encode());
        }
      } else {
        context.response().setStatusCode(500);
        context.response().end(
          new JsonObject()
            .put("success", false)
            .put("error", ar.cause().getMessage())
            .encode());
      }
    });

  }

  private static void configureLogging() {
    // It's OK to use MDC with static values
    MDC.put("application", "blog");
    MDC.put("version", "1.0.0");
    MDC.put("release", "canary");
    try {
      MDC.put("hostname", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      // Silent error, we can live without it
    }
  }

}
