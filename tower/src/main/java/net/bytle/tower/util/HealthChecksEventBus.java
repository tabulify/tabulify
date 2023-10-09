package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;

/**
 * An example implementation of Health checks
 * registered on the event bus to be used by the openapis
 * <a href="https://vertx.io/docs/vertx-health-check/java/">...</a>
 */
public class HealthChecksEventBus {

  /**
   * <a href="https://vertx.io/docs/vertx-core/java/#_addressing">...</a>
   */
  public static final String HEALTH_EVENT_BUS_ADDRESS = "health";

  /**
   * <a href="https://vertx.io/docs/vertx-core/java/#event_bus">...</a>
   */
  public static void registerHandlerToEventBus(Vertx vertx) {


    /**
     * Create the health checks
     */
    HealthChecks hc = HealthChecks.create(vertx);

    /**
     * First
     */
    hc.register(
      "my-procedure",
      promise -> promise.complete(Status.OK())
    );

    /**
     * Second
     */
    hc.register(
      "my-procedure",
      2000,
      promise -> promise.complete(Status.OK())
    );

    /**
     * Add the handler to the event bus
     */
    vertx
      .eventBus()
      .consumer(HEALTH_EVENT_BUS_ADDRESS, message -> hc.checkStatus()
        .onSuccess(message::reply)
        .onFailure(err -> message.fail(0, err.getMessage()))
      );

  }

}
