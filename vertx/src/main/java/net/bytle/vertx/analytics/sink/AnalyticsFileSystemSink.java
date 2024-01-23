package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;
import net.bytle.vertx.analytics.AnalyticsDelivery;
import net.bytle.vertx.analytics.AnalyticsEventDeliveryExecution;
import net.bytle.vertx.analytics.model.AnalyticsUser;

public class AnalyticsFileSystemSink extends AnalyticsSinkAbs {

  public AnalyticsFileSystemSink(AnalyticsDelivery analyticsDelivery) {
    super(analyticsDelivery);
  }

  @Override
  public String getName() {
    return "fileSystem";
  }

  @Override
  public AnalyticsMixPanelSink deliverUser(AnalyticsUser user, String ip) {
    return null;
  }


  @Override
  public Future<Void> processQueue() {

    for (AnalyticsEventDeliveryExecution eventDeliveryExecution : this.pullEventToDeliver(20)) {
      try {
        AnalyticsFileSystemLogger.log(eventDeliveryExecution.getEvent());
      } catch (Exception e) {
        eventDeliveryExecution.fatalError(e);
        continue;
      }
      eventDeliveryExecution.delivered();
    }
    return Future.succeededFuture();
  }


}
