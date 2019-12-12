package net.bytle.api.http;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.log.Log;

import java.util.Arrays;

/**
 *
 */
public class HttpServerVerticle extends AbstractVerticle {


  private static final Log LOGGER = Log.getLog(HttpServerVerticle.class);
  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";


  @Override
  public void start(Promise<Void> promise) throws Exception {


    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.get("/ip/info").handler(this::geoloc);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    server
      .requestHandler(router)
      .listen(portNumber, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + portNumber);
          promise.complete();
        } else {
          LOGGER.severe("Could not start a HTTP server " + ar.cause());
          promise.fail(ar.cause());
        }
      });
  }


  /**
   *
   * @param context
   * @param page
   * @param expectedKeys
   * @return
   *
   * Example:
   *     JsonObject json = context.getBodyAsJson();
   *     if (!validateJsonPageDocument(context, json, "key1", "key2")) {
   *       return;
   *     }
   */
  private boolean validateJsonPageDocument(RoutingContext context, JsonObject page, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(page::containsKey)) {
      LOGGER.severe("Bad page creation JSON payload: " + page.encodePrettily() + " from " + context.request().remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request payload").encode());
      return false;
    }
    return true;
  }


  private void geoloc(RoutingContext context) {
    String ip = context.request().getParam("ip");


    context.response().putHeader("Content-Type", "application/json");
    if (true) {
      context.response().setStatusCode(200);
      context.response().end(
        new JsonObject()
          .put("success", true)
          .encode());
    } else {
      context.response().end(
        new JsonObject()
          .put("success", false)
          .put("error", "error desc")
          .encode());
      context.response().setStatusCode(201);
    }
  }

}
