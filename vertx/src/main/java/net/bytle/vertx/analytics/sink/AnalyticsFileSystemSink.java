package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;
import net.bytle.vertx.analytics.AnalyticsDelivery;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsFileSystemSink extends AnalyticsSinkAbs {

  private final Map<String,AnalyticsEvent> eventInBatch;

  public AnalyticsFileSystemSink(AnalyticsDelivery analyticsDelivery) {
    super(analyticsDelivery);
    eventInBatch = new HashMap<>();
  }

  @Override
  public AnalyticsMixPanelSink deliverUser(AnalyticsUser user, String ip) {
    return null;
  }

  @Override
  public Integer getQueueSize() {
    return null;
  }

  @Override
  public Future<Void> processQueue() {

    for (AnalyticsEvent event : eventInBatch.values()) {
      try {
        AnalyticsFileSystemLogger.log(event);
      } catch (Exception e) {
        //this.handleFatalError(e, event);
      }
      this.eventInBatch.remove(event.getId());
    }
    //this.mapDb.commit();
    return Future.succeededFuture();
  }

  @Override
  public void addDelivery(AnalyticsEvent analyticsEvent) {
    this.eventInBatch.put(analyticsEvent.getId(), analyticsEvent);
  }

}
