package net.bytle.vertx.analytics.sink;

import net.bytle.vertx.analytics.AnalyticsDelivery;

public abstract class AnalyticsSinkAbs implements AnalyticsSink{


  private final AnalyticsDelivery analyticsDelivery;

  public AnalyticsSinkAbs(AnalyticsDelivery analyticsDelivery) {
    this.analyticsDelivery = analyticsDelivery;
  }

  public AnalyticsDelivery getAnalyticsDelivery() {
    return analyticsDelivery;
  }
}
