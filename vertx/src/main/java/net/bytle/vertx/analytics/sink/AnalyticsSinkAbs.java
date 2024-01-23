package net.bytle.vertx.analytics.sink;

import net.bytle.vertx.analytics.AnalyticsDelivery;
import net.bytle.vertx.analytics.AnalyticsEventDeliveryExecution;

import java.util.List;

public abstract class AnalyticsSinkAbs implements AnalyticsSink {


  private final AnalyticsDelivery analyticsDelivery;

  public AnalyticsSinkAbs(AnalyticsDelivery analyticsDelivery) {
    this.analyticsDelivery = analyticsDelivery;
  }

  public AnalyticsDelivery getAnalyticsDelivery() {
    return analyticsDelivery;
  }

  public List<AnalyticsEventDeliveryExecution> pullEventToDeliver(int batchNumber) {

    return this.analyticsDelivery.pullEventsToDeliver(batchNumber,this.getName());

  }

}
