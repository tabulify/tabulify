package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;
import net.bytle.vertx.analytics.model.AnalyticsUser;

public interface AnalyticsSink {

  /**
   * @return a name identifier
   */
  String getName();

  @SuppressWarnings("unused")
  AnalyticsMixPanelSink deliverUser(AnalyticsUser user, String ip);


  /**
   * Process the queue and return when finished
   * Every sink has its queue because it may have different
   * delivery pattern (batch event,...)
   */
  Future<Void> processQueue();



}
