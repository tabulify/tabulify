package net.bytle.api.http;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.api.db.DatabaseServiceInterface;
import net.bytle.api.db.DatabaseVerticle;

public class IpHandler implements Handler<RoutingContext> {

  private DatabaseServiceInterface dbService;

  /**
   * No singleton because we may start several Http Server Verticle
   * for test purpose
   * Use the below constructor please
   *
   * @param vertx
   */
  public IpHandler(Vertx vertx) {
    /**
     * Create the db service proxy that will send the event bus
     */
    dbService = DatabaseServiceInterface.createProxy(vertx, DatabaseVerticle.IP_QUEUE_NAME);
  }


  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    String ip = request.getParam("ip");
    if (ip == null) {
      ip = Https.getRealRemoteClient(request);
    }

    context.response().putHeader("Content-Type", "application/json");
    context.response().putHeader(Https.CACHE_CONTROL, Https.CACHE_CONTROL_NO_STORE);

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
