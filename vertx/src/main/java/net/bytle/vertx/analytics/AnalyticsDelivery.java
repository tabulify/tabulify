package net.bytle.vertx.analytics;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.Server;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;
import net.bytle.vertx.analytics.sink.AnalyticsLocalFileSystemSink;
import net.bytle.vertx.analytics.sink.AnalyticsMixPanelSink;
import net.bytle.vertx.analytics.sink.AnalyticsSink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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
  private final HashMap<String, AnalyticsDeliveryStatus<AnalyticsEvent>> eventsQueue = new HashMap<>();


  /**
   * The user queue
   */
  private final HashMap<String, AnalyticsDeliveryStatus<AnalyticsUser>> usersQueue = new HashMap<>();

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
      new AnalyticsLocalFileSystemSink(this)
    );
    for (AnalyticsSink analyticsSink : sinks) {
      this.sinks.add(analyticsSink);
      this.sinksName.add(analyticsSink.getName());
    }

    int sec10 = 10000;
    if (JavaEnvs.IS_IDE_DEBUGGING) {
      // 10 minutes
      sec10 = sec10 * 60;
    }
    server.getVertx().setPeriodic(sec10, sec10, jobId -> processSink());

  }

  public void processSink() {


    synchronized (this) {
      if (this.isRunning) {
        return;
      }
      this.isRunning = true;
    }

    List<Future<Void>> processesQueue = new ArrayList<>();
    for (AnalyticsSink analyticsSink : this.sinks) {
      processesQueue.add(analyticsSink.processEventQueue());
      processesQueue.add(analyticsSink.processUserQueue());
    }

    /**
     * Process the sink
     */
    Future.join(processesQueue)
      .onComplete(async -> {
        this.isRunning = false;
        if (async.failed()) {
          Throwable err = async.cause();
          LOGGER.error("The analytics processSink method has failed. Err:" + err.getMessage(), err);
        }
        CompositeFuture composite = async.result();
        for (int i = 0; i < composite.size(); i++) {
          if (composite.failed(i)) {
            String failedSinkName = this.sinks.get(i).getClass().getSimpleName();
            Throwable err = composite.causes().get(i);
            LOGGER.error("The analytics processSink (" + failedSinkName + ") has failed. Err:" + err.getMessage(), err);
          }
        }

      });

  }


  public AnalyticsDelivery addEventToDelivery(AnalyticsEvent analyticsEvent) {

    /**
     * It should never happen but this
     * is enough to get a difficult error
     * to debug from MixPanel
     */
    String name = analyticsEvent.getTypeName();
    if (name == null) {
      LOGGER.error("The analytics event has no name (" + analyticsEvent + ")");
      return this;
    }

    AnalyticsDeliveryStatus<AnalyticsEvent> analyticsEventDeliveryStatus = new AnalyticsDeliveryStatus<>(analyticsEvent, this.sinksName);
    this.eventsQueue.put(analyticsEvent.getGuid(), analyticsEventDeliveryStatus);
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
  public List<AnalyticsDeliveryExecution<AnalyticsEvent>> pullEventsToDeliver(int batchNumber, String sinkName) {
    return pullObjectToDeliver(eventsQueue, batchNumber, sinkName);
  }


  public AnalyticsDelivery addUserToDelivery(AnalyticsUser analyticsUser, String remoteIp) {

    /**
     * Ip is the only user request
     */
    analyticsUser.setRemoteIp(remoteIp);
    AnalyticsDeliveryStatus<AnalyticsUser> analyticsEventDeliveryStatus = new AnalyticsDeliveryStatus<>(analyticsUser, this.sinksName);
    this.usersQueue.put(analyticsUser.getGuid(), analyticsEventDeliveryStatus);
    return this;

  }

  public List<AnalyticsDeliveryExecution<AnalyticsUser>> pullUsersToDeliver(int batchNumber, String sinkName) {
    return pullObjectToDeliver(usersQueue, batchNumber, sinkName);
  }

  @NotNull
  private <T> List<AnalyticsDeliveryExecution<T>> pullObjectToDeliver(HashMap<String, AnalyticsDeliveryStatus<T>> objectQueue, int batchNumber, String sinkName) {
    List<AnalyticsDeliveryExecution<T>> pulled = new ArrayList<>();
    List<String> completedDelivery = new ArrayList<>();
    for (Map.Entry<String, AnalyticsDeliveryStatus<T>> entry : objectQueue.entrySet()) {

      String guid = entry.getKey();
      AnalyticsDeliveryStatus<T> analyticsEventDeliveryStatus = entry.getValue();
      if (analyticsEventDeliveryStatus.isComplete()) {
        completedDelivery.add(guid);
        continue;
      }
      if (analyticsEventDeliveryStatus.isDeliveredForSink(sinkName)) {
        continue;
      }

      pulled.add(new AnalyticsDeliveryExecution<>(analyticsEventDeliveryStatus, sinkName));
      if (pulled.size() >= batchNumber) {
        break;
      }

    }
    for (String guid : completedDelivery) {
      objectQueue.remove(guid);
    }
    return pulled;
  }
}
