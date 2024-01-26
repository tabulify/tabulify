package net.bytle.vertx.analytics.sink;

import io.vertx.core.Future;

public interface AnalyticsSink {

  /**
   * @return a name identifier for the sink (equivalent to the scheme in an uri)
   *
   */
  String getName();


  /**
   * Process the queues (event, user, ...) and return when finished
   */
  Future<Void> processQueue();



}
