package net.bytle.api.http;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.handler.LoggerFormat;
import net.bytle.api.Conf;
import net.bytle.api.EventBusChannels;
import net.bytle.api.db.DatabaseServiceInterface;
import net.bytle.api.db.DatabaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * <a href="https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients">Doc</a>
 */
public class VerticleHttpServer extends AbstractVerticle {


  protected static final Logger LOGGER = LogManager.getLogger();

  public static final int PORT_DEFAULT = 8083;
  public static final String LOCALHOST_DEFAULT = "0.0.0.0";
  private static final String PATH_DEFAULT = "v2/pokemon";
  private static final String PING_RESPONSE_DEFAULT = "pong from code";


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
      .setMessage(configuration.getString(Conf.Properties.PING_RESPONSE.toString(),PING_RESPONSE_DEFAULT));
    this.handlerFailure = new HandlerFailure();
    this.handlerPokemon = new HandlerPokemon(vertx)
      .setPokeApiUrl(
        configuration.getString(Conf.Properties.HOST.toString(), LOCALHOST_DEFAULT),
        configuration.getInteger(Conf.Properties.PORT.toString(), PORT_DEFAULT),
        configuration.getString(Conf.Properties.PATH.toString(), PATH_DEFAULT)
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
          pingHandler.setMessage(newConfiguration.getString(Conf.Properties.PING_RESPONSE.toString(),PING_RESPONSE_DEFAULT));
          handlerPokemon.setPokeApiUrl(
            newConfiguration.getString(Conf.Properties.HOST.toString(),LOCALHOST_DEFAULT),
            newConfiguration.getInteger(Conf.Properties.PORT.toString(), PORT_DEFAULT),
            newConfiguration.getString(Conf.Properties.PATH.toString(), PATH_DEFAULT)
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
    int portNumber = configuration.getInteger(Conf.Properties.PORT.toString(), PORT_DEFAULT);
    // 0.0.0.0 means listen on all available addresses
    String hostName = configuration.getString(Conf.Properties.HOST.toString(), LOCALHOST_DEFAULT);

    // Create the server
    // https://vertx.io/docs/vertx-core/java/#logging_network_activity
    HttpServerOptions options = new HttpServerOptions()
      .setLogActivity(true)
      .setHost(hostName)
      .setPort(portNumber);
    HttpServer server = vertx.createHttpServer(options);

    // Routing
    Router router = Router.router(vertx);

    // Logging Web Request
    router.route().handler(new WebLogger(LoggerFormat.DEFAULT));


    // Ip routing
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
    HttpServerRequest request = context.request();
    String ip = request.getParam("ip");
    if (ip ==null){
      ip = Https.getRealRemoteClient(request);
    }

    context.response().putHeader("Content-Type", "application/json");
    String finalIp = ip;
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
              .put("error", "country not found with the ip (" + finalIp + ")")
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


}
