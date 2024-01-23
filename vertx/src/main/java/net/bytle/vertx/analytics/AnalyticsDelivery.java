package net.bytle.vertx.analytics;

import io.vertx.core.Future;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.Server;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.sink.AnalyticsFileSystemSink;
import net.bytle.vertx.analytics.sink.AnalyticsMixPanelSink;
import net.bytle.vertx.analytics.sink.AnalyticsSink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage the queue to deliver the events to {@link AnalyticsSink}
 */
public class AnalyticsDelivery {

  static Logger LOGGER = LogManager.getLogger(AnalyticsDelivery.class);

  private final List<AnalyticsSink> sinks;
  private final Server server;
  private boolean logEventDelivery = false;

  public AnalyticsDelivery(Server server) throws ConfigIllegalException {

    this.server = server;
    if (JavaEnvs.IS_DEV) {
      this.logEventDelivery = true;
    }

    this.sinks = new ArrayList<>();
    AnalyticsMixPanelSink analyticsMixPanelSink = new AnalyticsMixPanelSink(this);
    this.sinks.add(analyticsMixPanelSink);
    AnalyticsFileSystemSink analyticsFileSystemSink = new AnalyticsFileSystemSink(this);
    this.sinks.add(analyticsFileSystemSink);

    int sec10 = 10000;
    server.getVertx().setPeriodic(sec10, sec10, jobId -> processSink());


  }

  public void processSink() {

    List<Future<Void>> processesQueue = new ArrayList<>();
    for (AnalyticsSink analyticsSink : this.sinks) {
      processesQueue.add(analyticsSink.processQueue());
    }

    /**
     * Process the sink sequentially
     */
    this.server.getFutureSchedulers()
      .createSequentialScheduler(Void.class)
      .join(processesQueue, null)
      .onFailure(err->LOGGER.error("The analytics processSink method has failed. Err:"+err.getMessage(), err))
      .onSuccess(async -> {
        if (async.hasFailed()) {
          String failedSinkName = this.sinks.get(async.getFailureIndex()).getClass().getSimpleName();
          Throwable err = async.getFailure();
          LOGGER.error("The analytics processSink ("+ failedSinkName + ") has failed. Err:"+err.getMessage(), err);
        }
      });

  }


  public AnalyticsDelivery addEventToDelivery(AnalyticsEvent analyticsEvent) {
    for (AnalyticsSink sync : this.sinks) {
      sync.addDelivery(analyticsEvent);
    }
    return this;
  }


  public Server getServer() {
    return this.server;
  }

  public boolean getLogEventDelivery() {
    return this.logEventDelivery;
  }

}
