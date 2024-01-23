package net.bytle.vertx.analytics;

import io.vertx.core.Future;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.Server;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.sink.AnalyticsFileSystemSink;
import net.bytle.vertx.analytics.sink.AnalyticsMixPanelSink;
import net.bytle.vertx.analytics.sink.AnalyticsSink;
import net.bytle.vertx.future.TowerFutureComposite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Manage the queue to deliver the events to {@link AnalyticsSink}
 */
public class AnalyticsDelivery {

  static Logger LOGGER = LogManager.getLogger(AnalyticsDelivery.class);

  /**
   * The event queue
   * (Don't use MapDb, when you iterate over the HTreeMap
   * it will create a new object instance each time for each value)
   */
  private final HashMap<String, AnalyticsEventDeliveryStatus> eventsQueue = new HashMap<>();


  private final List<AnalyticsSink> sinks = new ArrayList<>();


  private final Server server;
  private final Set<String> sinksName = new HashSet<>();
  private boolean logEventDelivery = false;
  private boolean isRunning = false;

  public AnalyticsDelivery(Server server) throws ConfigIllegalException {

    this.server = server;
    if (JavaEnvs.IS_DEV) {
      this.logEventDelivery = true;
    }

    /**
     * Sinks
     */

    List<AnalyticsSink> sinks = List.of(
      new AnalyticsMixPanelSink(this),
      new AnalyticsFileSystemSink(this)
    );
    for (AnalyticsSink analyticsSink : sinks) {
      this.sinks.add(analyticsSink);
      this.sinksName.add(analyticsSink.getName());
    }

    int sec10 = 10000;
    server.getVertx().setPeriodic(sec10, sec10, jobId -> processSink());

  }

  public void processSink() {


    synchronized (this){
      if(this.isRunning){
        return;
      }
      this.isRunning = true;
    }
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
      .onComplete(async->{
        this.isRunning = false;
        if(async.failed()){
          Throwable err = async.cause();
          LOGGER.error("The analytics processSink method has failed. Err:" + err.getMessage(), err);
          return;
        }
        TowerFutureComposite<Void> composite = async.result();
        if (composite.hasFailed()) {
          String failedSinkName = this.sinks.get(composite.getFailureIndex()).getClass().getSimpleName();
          Throwable err = composite.getFailure();
          LOGGER.error("The analytics processSink (" + failedSinkName + ") has failed. Err:" + err.getMessage(), err);
        }
      });

  }


  public AnalyticsDelivery addEventToDelivery(AnalyticsEvent analyticsEvent) {

    /**
     * It should never happen but this
     * is enough to get a difficult error
     * to debug from MixPanel
     */
    if (analyticsEvent.getName() == null) {
      LOGGER.error("The analytics event has no name (" + analyticsEvent + ")");
      return this;
    }
    AnalyticsEventDeliveryStatus analyticsEventDeliveryStatus = new AnalyticsEventDeliveryStatus(analyticsEvent, this.sinksName);
    this.eventsQueue.put(analyticsEvent.getId(), analyticsEventDeliveryStatus);
    return this;
  }


  public Server getServer() {
    return this.server;
  }

  public boolean getLogEventDelivery() {
    return this.logEventDelivery;
  }


  /**
   *
   * @param batchNumber - the maximum number of element to return
   * @param sinkName - the name of the sink
   * @return a collection to deliver
   */
  public List<AnalyticsEventDeliveryExecution> pullEventsToDeliver(int batchNumber, String sinkName) {

    List<AnalyticsEventDeliveryExecution> pulled = new ArrayList<>();
    List<AnalyticsEventDeliveryStatus> completedDelivery = new ArrayList<>();
    for(AnalyticsEventDeliveryStatus analyticsEventDeliveryStatus: this.eventsQueue.values()) {

      if (analyticsEventDeliveryStatus.isComplete()) {
        completedDelivery.add(analyticsEventDeliveryStatus);
        continue;
      }
      if (analyticsEventDeliveryStatus.isDeliveredForSink(sinkName)) {
        continue;
      }

      pulled.add(new AnalyticsEventDeliveryExecution(analyticsEventDeliveryStatus, sinkName));
      if (pulled.size() >= batchNumber) {
        break;
      }

    }
    for(AnalyticsEventDeliveryStatus analyticsEventDeliveryStatus: completedDelivery){
      this.eventsQueue.remove(analyticsEventDeliveryStatus.getAnalyticsEvent().getId());
    }
    return pulled;
  }

}
