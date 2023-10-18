package net.bytle.vertx;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.NoSecretException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Managed the tracking with:
 * * {@link AnalyticsEvent Events}. The things that happen in your product.
 * * {@link AnalyticsUser} The people who use your product.
 * * Company: Organizations that use your product.
 * * Traits are the properties of your users and companies (query filter and agg)
 */
public class AnalyticsTracker {


  private static final String PROJECT_TOKEN = "eraldy.mixpanel.project.token";
  private final MessageBuilder messageBuilder;
  private final Map<String, AnalyticsEvent> events = new HashMap<>();

  public AnalyticsTracker(String projectToken) {

    this.messageBuilder = new MessageBuilder(projectToken);
  }

  public static AnalyticsTracker createFromJsonObject(ConfigAccessor jsonConfig) throws NoSecretException {

    String projectToken = jsonConfig.getString(PROJECT_TOKEN);
    if (projectToken == null) {
      throw new NoSecretException("MixPanelTracker: A project token is mandatory to send the event. Add one in the conf file with the attribute (" + PROJECT_TOKEN + ")");
    }

    AnalyticsTracker analyticsTracker = new AnalyticsTracker(projectToken);
    return analyticsTracker;

  }

  public AnalyticsTracker addEvent(AnalyticsEvent analyticsEvent) {
    this.events.put(analyticsEvent.getId(), analyticsEvent);
    return this;
  }

  /**
   * @param user - the user
   * @param ip   - the ip when the user was created for geo-localization
   * @return the analytics tracker
   * Segment recommends that you make an Identify call:
   *
   * * After a user first registers
   * * After a user logs in
   * * When a user updates their info (for example, they change or add a new address)
   */
  public AnalyticsTracker deliverUser(AnalyticsUser user, String ip) {
    JsonObject props = AnalyticsMixPanel.toMixPanelUser(user, ip);
    return this;
  }

  /**
   *
   * GROUP_ID = Organisation
   * <a href="https://www.june.so/docs/quickstart/identify#companies">Company</a>
   */
  public AnalyticsTracker deliverOrganization() {
    return this;
  }


  public AnalyticsTracker deliverEvent() {

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
    for (AnalyticsEvent event : this.events.values()) {

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
      eventInBatch.add(event);
    }


    // Use an instance of MixpanelAPI to send the messages
    // to Mixpanel's servers.
    MixpanelAPI mixpanel = new MixpanelAPI();
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
      this.events.remove(event.getId());
    }
    return this;
  }

  public AnalyticsEvent createServerEvent(AnalyticsEventName eventName) {

    AnalyticsEvent analyticsEvent = new AnalyticsEvent();
    // Should be the device id
    analyticsEvent.setDeviceId("foobar");
    analyticsEvent.setId(UUID.randomUUID().toString());
    analyticsEvent.setName(eventName.toCamelCase());
    analyticsEvent.setCreationTime(DateTimeUtil.getNowUtc());
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
