package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;

public interface AnalyticsSink {


  @SuppressWarnings("unused")
  AnalyticsMixPanelSink deliverUser(AnalyticsUser user, String ip);

  /**
   * @return the number of event to deliver
   */
  Integer getQueueSize();


  /**
   * Process the queue and return when finished
   * Every sink has its queue because it may have different
   * delivery pattern (batch event,...)
   */
  Future<Void> processQueue();

  /**
   * Add an event to deliver
   * @param analyticsEvent - the event to deliver
   */
  void addDelivery(AnalyticsEvent analyticsEvent);

}
