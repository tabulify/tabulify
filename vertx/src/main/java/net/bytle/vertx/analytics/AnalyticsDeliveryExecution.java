package net.bytle.vertx.analytics;

import io.vertx.core.json.JsonObject;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An object that manage the execution cycle of an event.
 * The sink can set error or the delivery state
 */
public class AnalyticsDeliveryExecution<T> {

  private final static Logger LOGGER = LogManager.getLogger(AnalyticsDeliveryExecution.class);
  private final AnalyticsDeliveryStatus<T> analyticsDeliveryStatus;
  private final String sinkName;
  private final boolean logEvent;

  public AnalyticsDeliveryExecution(AnalyticsDeliveryStatus<T> analyticsDeliveryStatus, String sinkName) {
    this.analyticsDeliveryStatus = analyticsDeliveryStatus;
    this.sinkName = sinkName;
    this.logEvent = false;
  }

  public T getDeliveryObject() {
    return this.analyticsDeliveryStatus.getDeliveryObject();
  }


  public void fatalError(Exception e) {
    Integer failureCount = this.analyticsDeliveryStatus.incrementFatalErrorCounterForSink(sinkName);
    if (failureCount > 3) {
      this.analyticsDeliveryStatus.deliveredForSink(sinkName);
      LOGGER.error("The delivery of the event for the sink (" + sinkName + ") has failed", e);
    }
  }

  public void delivered() {
    this.analyticsDeliveryStatus.deliveredForSink(sinkName);
    if (logEvent) {
      Object object = analyticsDeliveryStatus.getDeliveryObject();
      if (object instanceof AnalyticsEvent) {
        LOGGER.info("The event (" + ((AnalyticsEvent) object).getTypeName() + ") for the sink (" + sinkName + ") has been delivered");
      }
      if(object instanceof AnalyticsUser){
        LOGGER.info("The user (" + ((AnalyticsUser) object).getEmail() + ") for the sink (" + sinkName + ") has been delivered");
      }
      LOGGER.info(JsonObject.mapFrom(object).toString());
    }
  }

}
