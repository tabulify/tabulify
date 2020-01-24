package net.bytle.api.http;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import net.bytle.api.Conf;
import net.bytle.api.EventBusChannels;
import net.bytle.api.db.DatabaseServiceInterface;
import net.bytle.api.db.DatabaseVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <a href="https://vertx.io/docs/vertx-core/java/#_writing_http_servers_and_clients">Doc</a>
 */
public class VerticleHttpServer extends AbstractVerticle {


  protected static final Logger LOGGER = LoggerFactory.getLogger(VerticleHttpServer.class);

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
      .setMessage(configuration.getString(Conf.Properties.PING_RESPONSE.toString(), PING_RESPONSE_DEFAULT));
    /**
     * Failure
     */
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
          pingHandler.setMessage(newConfiguration.getString(Conf.Properties.PING_RESPONSE.toString(), PING_RESPONSE_DEFAULT));
          handlerPokemon.setPokeApiUrl(
            newConfiguration.getString(Conf.Properties.HOST.toString(), LOCALHOST_DEFAULT),
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
    // a router should not be shared between verticles.
    Router router = Router.router(vertx);

    // Logging Web Request
    router.route().handler(new WebLogger(LoggerFormat.DEFAULT));

    // Cors
    String corsPattern = "https:\\/\\/(.*)gerardnico.com";
    String env = System.getProperty("env");
    if (env != null && env.equals("development")) {
      corsPattern = "http:\\/\\/localhost:([0-9]*)";
    }
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("Origin");
    allowedHeaders.add("content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");
    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);
    router.route().handler(
      CorsHandler
        .create(corsPattern)
        .allowedHeaders(allowedHeaders)
        .allowedMethods(allowedMethods)
    );

    // Ip routing
    IpHandler ipHandler = new IpHandler(vertx);
    router.get("/ip/:ip").handler(ipHandler);
    router.get("/ip/").handler(ipHandler);
    router.get("/ip").handler(ipHandler);

    // Analytics
    AnalyticsLogger analyticsLogger = new AnalyticsLogger(vertx);
    router.post(AnalyticsLogger.ANALYTICS_URL_PATH).handler(analyticsLogger);
    router.get(AnalyticsLogger.ANALYTICS_URL_PATH).handler(analyticsLogger);

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


}



