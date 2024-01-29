package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;
import net.bytle.vertx.analytics.AnalyticsDeliveryExecution;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;

import java.util.List;

public interface AnalyticsSink {

  /**
   * @return a name identifier for the sink (equivalent to the scheme in an uri)
   *
   */
  String getName();


  /**
   * Process the event queue and return when finished
   * This function should use the function {@link #pullEventToDeliver(int)} to get the events
   * to deliver
   */
  Future<Void> processEventQueue();


  List<AnalyticsDeliveryExecution<AnalyticsEvent>> pullEventToDeliver(int batchNumber);

  @SuppressWarnings("SameParameterValue")
  List<AnalyticsDeliveryExecution<AnalyticsUser>>  pullUserToDeliver(int batchNumber);

  /**
   * Process the user queue and return when finished
   * Event if the think does not deliver the user, it should acknowledge the reception
   * So that the delivery is successful.
   * This function should use the function {@link #pullUserToDeliver(int)} to get the users
   * to deliver
   */
  Future<Void> processUserQueue();

}
