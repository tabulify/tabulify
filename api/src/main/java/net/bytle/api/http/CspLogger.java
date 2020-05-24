package net.bytle.api.http;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.api.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The log are done with log4j
 * <p>
 * This handler will receive CSP report
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only
 * and log them
 */
public class CspLogger implements Handler<RoutingContext> {


  /**
   * The name of the logger
   */
  public static final String LOGGER_NAME = "csp";
  /**
   * The path of the url
   */
  public static final String ENDPOINT = "/" + LOGGER_NAME;
  /**
   * Where the log files should be
   */
  public static final Path LOG_DIR_PATH = Paths.get(Log.LOG_DIR_NAME, LOGGER_NAME);

  /**
   * The log files
   */
  public static final Path LOG_FILE_PATH = LOG_DIR_PATH.resolve(LOGGER_NAME+".jsonl");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());


  /**
   * No singleton because we may start several Http Server Verticle
   * for test purpose
   * Use the below constructor please
   */
  public CspLogger() {
  }


  @Override
  public void handle(RoutingContext context) {

    HttpServerRequest request = context.request();
    if (request.method().equals(HttpMethod.POST)) {
      request.bodyHandler(bodyHandler -> {

        JsonObject body = null;
        try {
          body = bodyHandler.toJsonObject();
        } catch (Exception e) {
          logger.error("Unable to decode the body as Json. Body=(" + bodyHandler.toString() + "), Error Message=(" + e.getMessage() + ")");
          context.response()
            .setStatusCode(422)
            .end();
        }

        logger.info(body.toString());

        context.response()
          .setStatusCode(200)
          .end();
      });
    } else {
      context.response()
        .setStatusCode(200)
        .end("");
    }


  }


}
