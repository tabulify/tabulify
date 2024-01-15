package net.bytle.vertx;

import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Health checks
 * A wrapper around <a href="https://vertx.io/docs/vertx-health-check/java/">...</a>
 */
public class HttpServerHealth {

  private static Logger LOGGER = LogManager.getLogger(HttpServerHealth.class);

  public static String PING_PATH = "/ping";

  public HttpServerHealth(HttpServer httpServer) {

    /**
     * Http Handler
     */
    Server server = httpServer.getServer();
    HealthChecks healthChecks = server
      .getServerHealthCheck()
      .getHealthChecks();

    HealthCheckHandler healthCheckHandler = HealthCheckHandler
      .createWithHealthChecks(healthChecks);
    httpServer.getRouter()
      .route(PING_PATH)
      .handler(healthCheckHandler);

    String url;
    try {
      url = new URL(httpServer.getHttpScheme(), "localhost", server.getListeningPort(), PING_PATH).toString();
    } catch (MalformedURLException e) {
      url = PING_PATH;
    }
    LOGGER.info("The health check end point has been added to " + url);

  }


}
