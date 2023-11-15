package net.bytle.vertx;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.auth.AuthUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Managed the tracking with:
 * * {@link AnalyticsEvent Events}. The things that happen in your product.
 * * {@link AnalyticsUser} The people who use your product.
 * * Company: Organizations that use your product.
 * * Traits are the properties of your users and companies (query filter and agg)
 */
public class AnalyticsTracker {

  static Logger LOGGER = LogManager.getLogger(AnalyticsTracker.class);

  private static final String PROJECT_TOKEN = "eraldy.mixpanel.project.token";
  private final MessageBuilder messageBuilder;
  private final HTreeMap<String, AnalyticsEvent> eventsQueue;
  private final MixpanelAPI mixpanel;
  private final MapDb mapDb;
  private boolean logEventDelivery = false;

  public AnalyticsTracker(Server server) throws ConfigIllegalException {

    String projectToken = server.getConfigAccessor().getString(PROJECT_TOKEN);
    if (projectToken == null) {
      throw new ConfigIllegalException("MixPanelTracker: A project token is mandatory to send the event. Add one in the conf file with the attribute (" + PROJECT_TOKEN + ")");
    }
    this.messageBuilder = new MessageBuilder(projectToken);

    this.mapDb = server.getMapDb();
    this.eventsQueue = mapDb
      .hashMap("event_queue", Serializer.STRING, AnalyticsEvent.class)
      .createOrOpen();

    int sec10 = 10000;
    server.getVertx().setPeriodic(sec10, sec10, jobId -> sendEventsInQueue());

    // Use an instance of MixpanelAPI to send the messages
    // to Mixpanel's servers.
    this.mixpanel = new MixpanelAPI();

    if (JavaEnvs.IS_DEV) {
      this.logEventDelivery = true;
    }

  }

  public static AnalyticsTracker createFromJsonObject(Server server) throws ConfigIllegalException {

    return new AnalyticsTracker(server);

  }

  public AnalyticsTracker addEventToQueue(AnalyticsEvent analyticsEvent) {
    this.eventsQueue.put(analyticsEvent.getId(), analyticsEvent);
    this.mapDb.commit();
    return this;
  }

  /**
   * @param user - the user
   * @param ip   - the ip when the user was created for geo-localization
   * @return the analytics tracker
   * Segment recommends that you make an Identify call:
   * <p>
   * * After a user first registers
   * * After a user logs in
   * * When a user updates their info (for example, they change or add a new address)
   */
  public AnalyticsTracker deliverUser(AnalyticsUser user, String ip) {
    JsonObject props = AnalyticsMixPanel.toMixPanelUser(user, ip);
    return this;
  }

  /**
   * GROUP_ID = Organisation
   * <a href="https://www.june.so/docs/quickstart/identify#companies">Company</a>
   */
  public AnalyticsTracker deliverOrganization() {
    return this;
  }


  public AnalyticsTracker sendEventsInQueue() {

    if (this.eventsQueue.size() == 0) {
      return this;
    }

    // Gather together a bunch of messages into a single
    // ClientDelivery. This can happen in a separate thread
    // or process from the call to MessageBuilder.event()
    ClientDelivery delivery = new ClientDelivery();

    /**
     * The error of the API returns the id of the inserted
     * message.
     * https://developer.mixpanel.com/reference/import-events#example-of-a-validation-error
     * Therefore, we create a list
     */
    List<AnalyticsEvent> eventInBatch = new ArrayList<>();
    for (AnalyticsEvent event : this.eventsQueue.values()) {

      event.setSendingTime(DateTimeUtil.getNowUtc());
      JsonObject props = AnalyticsMixPanel.toMixpanelPropsWithoutUserId(event);

      // Create an event
      // https://docs.mixpanel.com/docs/tracking/how-tos/identifying-users#what-is-distinct-id
      // https://docs.mixpanel.com/docs/tracking/reference/distinct-id-limits
      // The purpose of distinct_id is to provide a single, unified identifier for a user across devices and sessions.
      // This helps Mixpanel compute metrics like Daily Active Users accurately: when two events have the same value of distinct_id, they are considered as being performed by 1 unique user.
      JSONObject mixpanelEvent = messageBuilder.event(
        AnalyticsMixPanel.toMixPanelUserDistinctId(event),
        event.getName(),
        new JSONObject(props.getMap())
      );
      if (!delivery.isValidMessage(mixpanelEvent)) {
        continue;
      }
      delivery.addMessage(mixpanelEvent);
      if (this.logEventDelivery) {
        LOGGER.info("The event " + event + " was added for delivery");
      }
      eventInBatch.add(event);
    }


    try {
      // https://developer.mixpanel.com/reference/import-events
      mixpanel.deliver(delivery);
    } catch (IOException e) {
      // Example: Here it's `Can't communicate with Mixpanel` https://github.com/mixpanel/mixpanel-java/blob/master/src/demo/java/com/mixpanel/mixpanelapi/demo/MixpanelAPIDemo.java#L57C45-L57C77
      // Do we get the error?
      // https://developer.mixpanel.com/reference/import-events#example-of-a-validation-error
      // See impl: https://github.com/mixpanel/mixpanel-java/blob/master/src/main/java/com/mixpanel/mixpanelapi/MixpanelAPI.java#L195
      throw new RuntimeException(e);
    }
    for (AnalyticsEvent event : eventInBatch) {
      this.eventsQueue.remove(event.getId());
    }
    this.mapDb.commit();
    return this;
  }

  public ServerEventBuilder eventBuilder(AnalyticsEventName eventName) {

    return new ServerEventBuilder(eventName);

  }

  public class ServerEventBuilder {
    private final AnalyticsEventName eventName;
    private AuthUser authUser;
    private RoutingContext routingContext;

    public ServerEventBuilder(AnalyticsEventName eventName) {
      this.eventName = eventName;
    }

    /**
     * Send the event to the queue
     * (the event is processed async)
     */
    public void sendEventAsync() {

      AnalyticsTracker.this.addEventToQueue(buildEvent());

    }

    public ServerEventBuilder setUser(AuthUser authUser) {
      this.authUser = authUser;
      return this;
    }

    public ServerEventBuilder setRoutingContext(RoutingContext routingContext) {
      this.routingContext = routingContext;
      return this;
    }

    public AnalyticsEvent buildEvent() {
      AnalyticsEvent analyticsEvent = new AnalyticsEvent();
      analyticsEvent.setId(UUID.randomUUID().toString());
      analyticsEvent.setName(this.eventName.toCamelCase());
      analyticsEvent.setCreationTime(DateTimeUtil.getNowUtc());

      if (this.authUser != null) {
        AnalyticsUser analyticsUser = new AnalyticsUser();
        analyticsUser.setId(authUser.getSubject());
        analyticsUser.setEmail(authUser.getSubjectEmail());
        analyticsUser.setGivenName(authUser.getSubjectGivenName());
        analyticsUser.setAvatar(authUser.getSubjectAvatar());
        analyticsEvent.setUser(analyticsUser);
      }

      /**
       * Context
       */
      AnalyticsEventContext analyticsEventContext = new AnalyticsEventContext();
      analyticsEventContext.setChannel(AnalyticsEventChannel.SERVER);
      analyticsEvent.setContext(analyticsEventContext);

      /**
       * OS
       */
      AnalyticsOperatingSystem analyticsOperatingSystem = new AnalyticsOperatingSystem();
      analyticsOperatingSystem.setName(System.getProperty("os.name"));
      analyticsOperatingSystem.setVersion(System.getProperty("os.version"));
      analyticsOperatingSystem.setArch(System.getProperty("os.arch"));
      analyticsEventContext.setOs(analyticsOperatingSystem);
      return analyticsEvent;
    }
  }

}
