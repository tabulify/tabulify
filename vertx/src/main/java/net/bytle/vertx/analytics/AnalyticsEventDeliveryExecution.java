package net.bytle.vertx.analytics;

import io.vertx.core.json.JsonObject;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An object that manage the execution cycle of an event.
 * The sink can set error or the delivery state
 */
public class AnalyticsEventDeliveryExecution {

  private final static Logger LOGGER = LogManager.getLogger(AnalyticsEventDeliveryExecution.class);
  private final AnalyticsEventDeliveryStatus analyticsEventDeliveryStatus;
  private final String sinkName;

  public AnalyticsEventDeliveryExecution(AnalyticsEventDeliveryStatus analyticsEventDeliveryStatus, String sinkName) {
    this.analyticsEventDeliveryStatus = analyticsEventDeliveryStatus;
    this.sinkName = sinkName;
  }

  public AnalyticsEvent getEvent() {
    return this.analyticsEventDeliveryStatus.getAnalyticsEvent();
  }


  public void fatalError(Exception e) {
    Integer failureCount = this.analyticsEventDeliveryStatus.incrementFatalErrorCounterForSink(sinkName);
    if (failureCount > 3) {
      this.analyticsEventDeliveryStatus.deliveredForSink(sinkName);
      LOGGER.error("The delivery of the event for the sink (" + sinkName + ") has failed", e);
    }
  }

  public void delivered() {
    this.analyticsEventDeliveryStatus.deliveredForSink(sinkName);
    if (JavaEnvs.IS_DEV) {
      AnalyticsEvent analyticsEvent = analyticsEventDeliveryStatus.getAnalyticsEvent();
      LOGGER.info("The event (" + analyticsEvent.getName() + ") for the sink (" + sinkName + ") has been delivered");
      LOGGER.info(JsonObject.mapFrom(analyticsEvent).toString());
    }
  }
}
