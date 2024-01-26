package net.bytle.vertx.analytics.sink;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import net.bytle.vertx.analytics.AnalyticsDelivery;
import net.bytle.vertx.analytics.AnalyticsEventDeliveryExecution;
import net.bytle.vertx.analytics.model.AnalyticsEventApp;
import net.bytle.vertx.analytics.model.AnalyticsEventRequest;

public class AnalyticsLocalFileSystemSink extends AnalyticsSinkAbs {

  private final JsonMapper noHandleMixin;

  public AnalyticsLocalFileSystemSink(AnalyticsDelivery analyticsDelivery) {

    super(analyticsDelivery);
    this.noHandleMixin = analyticsDelivery.getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(AnalyticsEventApp.class, AnalyticsEventAppWithoutHandleMixin.class)
      .addMixIn(AnalyticsEventRequest.class, AnalyticsEventRequestWithoutHandleMixin.class)
      .build();
  }

  @Override
  public String getName() {
    return "file";
  }


  @Override
  public Future<Void> processQueue() {

    for (AnalyticsEventDeliveryExecution eventDeliveryExecution : this.pullEventToDeliver(20)) {
      try {
        AnalyticsFileSystemLogger.log(eventDeliveryExecution.getEvent(), this.noHandleMixin);
      } catch (Exception e) {
        eventDeliveryExecution.fatalError(e);
        continue;
      }
      eventDeliveryExecution.delivered();
    }
    return Future.succeededFuture();
  }


}
