package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;
import net.bytle.vertx.analytics.AnalyticsDelivery;
import net.bytle.vertx.analytics.AnalyticsDeliveryExecution;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;

import java.util.List;

public abstract class AnalyticsSinkAbs implements AnalyticsSink {


  private final AnalyticsDelivery analyticsDelivery;

  public AnalyticsSinkAbs(AnalyticsDelivery analyticsDelivery) {
    this.analyticsDelivery = analyticsDelivery;
  }

  public AnalyticsDelivery getAnalyticsDelivery() {
    return analyticsDelivery;
  }

  @Override
  public List<AnalyticsDeliveryExecution<AnalyticsEvent>> pullEventToDeliver(int batchNumber) {

    return this.analyticsDelivery.pullEventsToDeliver(batchNumber,this.getName());

  }

  @Override
  @SuppressWarnings("SameParameterValue")
  public List<AnalyticsDeliveryExecution<AnalyticsUser>>  pullUserToDeliver(int batchNumber) {
    return this.analyticsDelivery.pullUsersToDeliver(batchNumber,this.getName());
  }

  @Override
  public Future<Void> processUserQueue() {

    /**
     * By default, no processing
     */
    for (AnalyticsDeliveryExecution<AnalyticsUser> eventDeliveryExecution : this.pullUserToDeliver(20)) {
      eventDeliveryExecution.delivered();
    }
    return Future.succeededFuture();

  }
}
