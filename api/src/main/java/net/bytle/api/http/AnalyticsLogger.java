package net.bytle.api.http;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.api.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * The log are done with the <a href="http://logging.apache.org/log4j/2.x/manual/appenders.html#RoutingAppender">Routes</a>
 *
 */
public class AnalyticsLogger implements Handler<RoutingContext> {


  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Static field
  public static final String ANALYTICS_NAME = "analytics";
  public static final String ANALYTICS_URL_PATH = "/"+ANALYTICS_NAME;
  public static final Path ANALYTICS_LOG_PATH = Paths.get(Log.LOG_DIR_NAME, ANALYTICS_NAME);

  private final Vertx vertx;

  /**
   * No singleton because we may start several Http Server Verticle
   * for test purpose
   * Use the below constructor please
   *
   * @param vertx
   */
  public AnalyticsLogger(Vertx vertx) {
    this.vertx = vertx;
  }



  @Override
  public void handle(RoutingContext context) {

    HttpServerRequest request = context.request();
    if (request.method().equals(HttpMethod.POST)) {
      request.bodyHandler(bodyHandler -> {

        // Adding extra server info
        final JsonObject body = bodyHandler.toJsonObject();
        body.put("received_at", new Date().toInstant()); // UTC time
        JsonObject eventContext = body.getJsonObject("context");
        if (eventContext == null) {
          eventContext = new JsonObject();
          body.put("context", eventContext);
        }
        eventContext.put("ip", Https.getRealRemoteClient(request));
        // Log
        String eventName = body.getString("event_name");
        if (eventName == null) {
          eventName = "unknown event";
        }
        // the event-slug is used in the name of the file and directory
        String event_slug = eventName.trim().toLowerCase().replace(" ", "_");
        MDC.put("event_slug", event_slug);
        logger.info(body.toString());

        context.response()
          .setStatusCode(200)
          .end();
      });
    } else {
      context.response()
        .setStatusCode(200)
        .end("analytics");
    }


  }

  private void addCurrentDate(JsonObject jsonObject) {

  }

}
