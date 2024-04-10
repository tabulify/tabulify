package net.bytle.vertx.analytics.sink;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import net.bytle.dns.DnsException;
import net.bytle.dns.DnsIp;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyCase;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.Server;
import net.bytle.vertx.analytics.AnalyticsDelivery;
import net.bytle.vertx.analytics.AnalyticsDeliveryExecution;
import net.bytle.vertx.analytics.AnalyticsException;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsEventClient;
import net.bytle.vertx.analytics.model.AnalyticsEventRequest;
import net.bytle.vertx.analytics.model.AnalyticsUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MixPanel - modify the time based on the project time zone!
 * Be sure to have UTC
 * <p>
 * Mixpanel utility class
 * based on:
 * <a href="https://github.com/mixpanel/mixpanel-java/blob/master/src/demo/java/com/mixpanel/mixpanelapi/demo/MixpanelAPIDemo.java"></a>
 */
public class AnalyticsMixPanelSink extends AnalyticsSinkAbs {

  private static final String MIX_PANEL_PROJECT_TOKEN = "eraldy.mixpanel.project.token";
  private static final String MIXPANEL_KEY_CASE_CONF = "mixpanel.key.case";
  private static final String MIX_PANEL_BATCH_NUMBER = "mixpanel.delivery.batch.size";

  /**
   * Max 2000 events per batch
   * as stated in the doc
   */
  private static final int MAX_BATCH_SIZE = 2000;

  private final KeyCase keyCase;
  static Logger LOGGER = LogManager.getLogger(AnalyticsMixPanelSink.class);

  private final MixpanelAPI mixpanel;
  private final MessageBuilder messageBuilder;
  private final Integer deliveryBatchSize;
  private final String customPropertyAppHandleKey;
  private final String customPropertyRealmHandle;
  private final String customPropertyOrganizationHandle;
  private final String customPropertyUserEmailKeyNormalized;
  private final String customPropertyAppIdKey;
  private final String customPropertyRealmId;
  private final String customPropertyOrganizationId;
  private final String customPropertyFlowId;
  private final String customPropertyFlowHandle;
  private final String customPropertySessionId;


  public AnalyticsMixPanelSink(AnalyticsDelivery analyticsDelivery) throws ConfigIllegalException {
    super(analyticsDelivery);

    Server server = analyticsDelivery.getServer();

    /**
     * When sending the key to mixpanel,
     * it will show you it has name
     * We normalize them then as Handle so that it's easier for human
     * to read them
     */
    String keyCase = server.getConfigAccessor().getString(MIXPANEL_KEY_CASE_CONF, KeyCase.HANDLE.toString());
    KeyCase wordCase;
    try {
      wordCase = Casts.cast(keyCase, KeyCase.class);
    } catch (CastException e) {
      throw new ConfigIllegalException("The value (" + keyCase + ") from the configuration (" + MIXPANEL_KEY_CASE_CONF + ") is not valid. The possibles values are: " + Enums.toConstantAsStringCommaSeparated(KeyCase.class), e);
    }
    this.keyCase = wordCase;
    String projectToken = server.getConfigAccessor().getString(MIX_PANEL_PROJECT_TOKEN);
    if (projectToken == null) {
      throw new ConfigIllegalException("MixPanel: A project token is mandatory to send the event. Add one in the conf file with the attribute (" + MIX_PANEL_PROJECT_TOKEN + ")");
    }
    this.messageBuilder = new MessageBuilder(projectToken);

    // Use an instance of MixpanelAPI to send the messages
    // to Mixpanel's servers.
    this.mixpanel = new MixpanelAPI();

    this.deliveryBatchSize = server.getConfigAccessor().getInteger(MIX_PANEL_BATCH_NUMBER, 20);
    if (this.deliveryBatchSize > MAX_BATCH_SIZE) {
      throw new ConfigIllegalException("MixPanel: The delivery batch size value (" + this.deliveryBatchSize + ") from the configuration (" + MIX_PANEL_PROJECT_TOKEN + ") s greater than " + MAX_BATCH_SIZE);
    }

    /**
     * The property customized
     */
    this.customPropertyUserEmailKeyNormalized = KeyNormalizer.createFromString("User Email").toCase(this.keyCase);
    this.customPropertyAppIdKey = KeyNormalizer.createFromString("App Guid").toCase(this.keyCase);
    this.customPropertyAppHandleKey = KeyNormalizer.createFromString("App Handle").toCase(this.keyCase);
    this.customPropertyRealmId = KeyNormalizer.createFromString("Realm Guid").toCase(this.keyCase);
    this.customPropertyRealmHandle = KeyNormalizer.createFromString("Realm Handle").toCase(this.keyCase);
    this.customPropertyOrganizationId = KeyNormalizer.createFromString("Organization Guid").toCase(this.keyCase);
    this.customPropertyOrganizationHandle = KeyNormalizer.createFromString("Organization Handle").toCase(this.keyCase);
    this.customPropertyFlowId = KeyNormalizer.createFromString("Flow Guid").toCase(this.keyCase);
    this.customPropertyFlowHandle = KeyNormalizer.createFromString("Flow Handle").toCase(this.keyCase);
    this.customPropertySessionId = KeyNormalizer.createFromString("Session Id").toCase(this.keyCase);

  }

  /**
   * @param user - the user
   * @return the json mixpanel object
   * <a href="https://docs.mixpanel.com/docs/data-structure/user-profiles#reserved-user-properties">MixPanel User</a>
   * <a href="https://docs.mixpanel.com/docs/data-structure/user-profiles#reserved-profile-properties">MixPanel built-in</a>
   */
  public JsonObject toMixPanelUserWithoutId(AnalyticsUser user) {

    JsonObject props = new JsonObject();
    // $group_id, the group identifier, for group profiles, as these are the canonical identifiers in Mixpanel.
    props.put("$email", user.getEmail());
    props.put("$name", user.getName());
    props.put("$last_name", user.getGivenName());
    props.put("$last_name", user.getFamilyName());

    URI avatar = user.getAvatar();
    if (avatar != null) {
      props.put("$avatar", avatar.toString());
    }
    /**
     * first event in Mixpanel
     * The timezone of the project should be UTC
     */
    LocalDateTime creationTime = user.getCreationTime();
    if (creationTime != null) {
      props.put("$mp_first_event_time", creationTime.toString());
    }

    /**
     * ip, determine $city, $region, $country_code and $timezone
     */
    this.addIp(props, user.getRemoteIp());

    // ???
    //props.put("$ignore_time", "true");

    // Custom
    // props.put("Plan", "Premium");

    return props;
  }

  /**
   * See
   * <a href="https://docs.mixpanel.com/docs/tracking/reference/default-properties">MixPanel Properties</a>
   * and
   * <a href="https://docs.mixpanel.com/docs/other-bits/tutorials/developers/mixpanel-for-developers-fundamentals">For Developers</a>
   * and
   * <a href="https://docs.mixpanel.com/docs/tracking-methods/sdks/java">Java Example</a>
   * @param event the event
   * @return the JSON for Mixpanel without the user id (ie distinct id) because it's mandatory to add it in the event function signature of Mixpanel
   */
  protected JsonObject toMixpanelPropsWithoutUserId(AnalyticsEvent event) {

    JsonObject props = new JsonObject();

    /**
     * $device_id: The anonymous / device id
     */
    AnalyticsEventRequest request = event.getRequest();
    String agentId = request.getAgentGuid();
    if (agentId != null) {
      props.put("$device_id", agentId);
    }

    /**
     * $user_id
     */
    String userId = event.getUser().getUserGuid();
    if (userId != null) {
      props.put("$user_id", userId);
    }
    String userEmail = event.getUser().getUserEmail();
    if (userEmail != null) {
      props.put(this.customPropertyUserEmailKeyNormalized, userEmail);
    }

    /**
     * $insert_id: A unique identifier for the event,
     * used to deduplicate events that are accidentally sent multiple times.
     */
    props.put("$insert_id", event.getGuid());

    /**
     * IP
     * Geolocation is by default turned on
     * https://docs.mixpanel.com/docs/tracking/how-tos/privacy-friendly-tracking#disabling-geolocation
     */
    String ip = request.getRemoteIp();
    this.addIp(props, ip);


    /**
     * Request
     * Session Properties
     * <a href="https://docs.mixpanel.com/docs/features/sessions#session-properties'>Session</a>
     */
    String sessionId = request.getSessionId();
    if (sessionId != null) {
      props.put(this.customPropertySessionId, sessionId);
    }
    URI originUri = request.getOriginUri();
    if (originUri != null) {
      try {
        props.put("$current_url", originUri.toURL().toString());
      } catch (MalformedURLException e) {
        // not an url
      }
    }

    /**
     * Flow
     */
    String flowId = request.getFlowGuid();
    if (flowId != null) {
      props.put(customPropertyFlowId, flowId);
    }
    String flowHandle = request.getFlowHandle();
    if (flowHandle != null) {
      props.put(customPropertyFlowHandle, flowHandle);
    }

    /**
     * Channel (Paid Search, ..)
     * https://docs.mixpanel.com/docs/features/custom-properties#grouping-marketing-channels
     * props.put("channel");
     * Utm comes from <a href="https://docs.mixpanel.com/docs/features/sessions#session-properties'>Session Properties</a>
     */
    String utmCampagne = event.getUtm().getUtmCampaign();
    if (utmCampagne != null) {
      props.put("utm_campaign", utmCampagne);
    }
    String utmContent = event.getUtm().getUtmContent();
    if (utmContent != null) {
      props.put("utm_content", utmContent);
    }
    String utmSource = event.getUtm().getUtmSource();
    if (utmSource != null) {
      props.put("utm_source", utmSource);
    }

    /**
     * State and time
     * A date can in Epoch Sec or in Iso String
     * <p>
     * Note: The iso string date is changed based on the project timezone.
     * If the date in the data is not good, verify that the timezone of the project is UTC.
     */
    /**
     * Creation Time is known as $time in PixPanel
     * The Value example given in the help when selecting an event is 2011-01T00:00:00Z
     * ie Timestamp.createFromLocalDateTime(event.getState().getCreationTime()).toIsoString()+"Z";
     * The event is processed successfully
     * <p>
     * BUT the gui is not happy.
     * We have discovered that ultimately they store all date in Epoch Sec (a download give you epoch data)
     * Setting it as Epoch, it just works.
     */
    Long creationTimeIso = Timestamp.createFromLocalDateTime(event.getState().getEventCreationTime()).toEpochSec();
    props.put("$time", creationTimeIso);


    /**
     * Group Analytics is an add-on
     * We use for now custom properties
     * https://docs.mixpanel.com/docs/tracking-methods/sdks/java#group-analytics
     * https://docs.mixpanel.com/docs/data-structure/advanced/group-analytics
     */
    AnalyticsEventClient app = event.getClient();
    String appId = app.getAppGuid();
    if (appId != null) {
      props.put(customPropertyAppIdKey, appId);
    }
    String appHandle = app.getAppHandle();
    if (appHandle != null) {
      props.put(customPropertyAppHandleKey, appHandle);
    }
    // realm and orga info are always set on the app
    // ie if a user is known they are updated
    String realmId = app.getAppRealmGuid();
    if (realmId != null) {
      props.put(customPropertyRealmId, realmId);
    }
    String realmHandle = app.getAppRealmHandle();
    if (realmHandle != null) {
      props.put(customPropertyRealmHandle, realmHandle);
    }
    String organisationId = app.getAppOrganisationGuid();
    if (organisationId != null) {
      props.put(customPropertyOrganizationId, organisationId);
    }
    String organisationHandle = app.getAppOrganisationHandle();
    if (organisationHandle != null) {
      props.put(customPropertyOrganizationHandle, organisationHandle);
    }


    /**
     * Additional properties along with events
     */
    for (Map.Entry<String, Object> entry : event.getAttr().entrySet()) {
      /**
       * Does the `toString` work with MixPanel Data???
       * https://docs.mixpanel.com/docs/other-bits/tutorials/developers/mixpanel-for-developers-fundamentals#supported-data-types
       */
      Object value = entry.getValue();
      if (value == null) {
        if (JavaEnvs.IS_DEV) {
          LOGGER.error("The value of the key (" + entry.getKey() + ") for the event (" + event.getTypeName() + ") is null. The value was ignored.");
        }
        continue;
      }
      String valueString = value.toString();
      if (valueString.isBlank()) {
        continue;
      }
      String snakeCaseKey = KeyNormalizer.createFromString(entry.getKey()).toCase(keyCase);
      props.put(snakeCaseKey, valueString);
    }
    return props;

  }

  /**
   * ip, determine $city, $region, $country_code and $timezone
   */
  private void addIp(JsonObject props, String ip) {
    ip = getIpOrRandomForDev(ip);
    if (ip == null) {
      return;
    }
    props.put("ip", ip);
  }

  /**
   * @param ip - the ip
   * @return return the IP or a random ip if the IP is a loopback ip in dev
   */
  private String getIpOrRandomForDev(String ip) {

    if (!JavaEnvs.IS_DEV) {
      // return in production
      return ip;
    }
    // return random if null
    if (ip == null) {
      return DnsIp.createRandomIpv4().toString();
    }
    try {
      DnsIp dnsIp = DnsIp.createFromString(ip);
      if (dnsIp.getInetAddress().isLoopbackAddress()) {
        //
        // a random ip just to test that the ip is taken into account
        return DnsIp.createRandomIpv4().toString();
      }
    } catch (DnsException e) {
      // should not
    }
    return ip;

  }

  /**
   * The doc is here:<a href="https://docs.mixpanel.com/docs/tracking/how-tos/identifying-users">Identifying user and what is distinct Id?</a>
   * <p>
   * What is strange is that it seems that this is the library that does that
   * but the distinct id is asked for the construction of an event.
   * <p>
   * distinct_id is an identifier set by Mixpanel based on the combination of $device_id and $user_id.
   * The purpose of distinct_id is to provide a single, unified identifier for a user across devices and sessions.
   * <p>
   * This helps Mixpanel compute metrics like Daily Active Users accurately: when two events have the same value of distinct_id, they are considered as being performed by 1 unique user.
   * <p>
   * Simplified ID Merge (default configuration): distinct_id will be the $user_id if present, otherwise will be $device:<$device_id>.
   * <p>
   *
   * @param event - the event
   * @return the user id
   */
  protected String toMixPanelUserDistinctId(AnalyticsEvent event) {
    String userId = event.getUser().getUserGuid();
    if (userId != null) {
      return userId;
    }
    return event.getClient().getAppGuid();
  }


  @Override
  public String getName() {
    return "mixpanel";
  }

  /**
   * @param user - the user
   */
  public AnalyticsMixPanelSink deliverUser(AnalyticsUser user) throws AnalyticsException {

    JsonObject props = this.toMixPanelUserWithoutId(user);
    JSONObject mixPanelJsonObject = new JSONObject(props.getMap());
    // https://docs.mixpanel.com/docs/tracking-methods/sdks/java#setting-profile-properties
    JSONObject update = this.messageBuilder.set(user.getGuid(), mixPanelJsonObject);
    try {
      this.mixpanel.sendMessage(update);
    } catch (IOException e) {
      throw new AnalyticsException(e);
    }
    return this;
  }

  /**
   * GROUP_ID = Organisation
   * <a href="https://www.june.so/docs/quickstart/identify#companies">Company</a>
   */
  @SuppressWarnings("unused")
  public void deliverOrganization() {
  }

  public JSONObject buildEvent(AnalyticsEvent event) {
    JsonObject vertxJsonObject = this.toMixpanelPropsWithoutUserId(event);
    JSONObject mixPanelJsonObject = new JSONObject(vertxJsonObject.getMap());
    String mixPanelUserDistinctId = this.toMixPanelUserDistinctId(event);
    String name = event.getTypeName();
    return this.messageBuilder.event(
      mixPanelUserDistinctId,
      name,
      mixPanelJsonObject
    );
  }


  @Override
  public Future<Void> processEventQueue() {


    processEvent();

    return Future.succeededFuture();

  }

  @Override
  public Future<Void> processUserQueue() {
    for (AnalyticsDeliveryExecution<AnalyticsUser> eventDeliveryExecution : this.pullUserToDeliver(20)) {

      try {
        this.deliverUser(eventDeliveryExecution.getDeliveryObject());
      } catch (AnalyticsException e) {
        eventDeliveryExecution.fatalError(e);
        continue;
      }
      eventDeliveryExecution.delivered();

    }
    return Future.succeededFuture();
  }

  /**
   * This function process the event queue
   */
  private void processEvent() {

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
    List<AnalyticsDeliveryExecution<AnalyticsEvent>> eventInBatch = new ArrayList<>();

    List<AnalyticsDeliveryExecution<AnalyticsEvent>> analyticsDeliveryExecutions = this.pullEventToDeliver(deliveryBatchSize);
    for (AnalyticsDeliveryExecution<AnalyticsEvent> eventDelivery : analyticsDeliveryExecutions) {

      try {

        AnalyticsEvent event = eventDelivery.getDeliveryObject();

        event.getState().setEventSendingTime(DateTimeService.getNowInUtc());

        // Create an event
        // https://docs.mixpanel.com/docs/tracking/how-tos/identifying-users#what-is-distinct-id
        // https://docs.mixpanel.com/docs/tracking/reference/distinct-id-limits
        // The purpose of distinct_id is to provide a single, unified identifier for a user across devices and sessions.
        // This helps Mixpanel compute metrics like Daily Active Users accurately: when two events have the same value of distinct_id, they are considered as being performed by 1 unique user.
        JSONObject mixpanelEvent = this.buildEvent(event);
        if (!delivery.isValidMessage(mixpanelEvent)) {
          eventDelivery.fatalError(new InternalException("The message is not valid for MixPanel"));
          continue;
        }
        delivery.addMessage(mixpanelEvent);
        if (this.getAnalyticsDelivery().getLogEventDelivery()) {
          LOGGER.info("The event " + event + " was added for delivery");
        }
        eventInBatch.add(eventDelivery);
      } catch (Exception e) {
        eventDelivery.fatalError(e);
      }
    }

    Exception mixpanelDeliveryException = null;
    try {
      // https://developer.mixpanel.com/reference/import-events
      this.mixpanel.deliver(delivery);
    } catch (IOException e) {
      // Example: Here it's `Can't communicate with Mixpanel`
      // https://github.com/mixpanel/mixpanel-java/blob/master/src/demo/java/com/mixpanel/mixpanelapi/demo/MixpanelAPIDemo.java#L57C45-L57C77
      // Do we get the error?
      // https://developer.mixpanel.com/reference/import-events#example-of-a-validation-error
      // See impl: https://github.com/mixpanel/mixpanel-java/blob/master/src/main/java/com/mixpanel/mixpanelapi/MixpanelAPI.java#L195
      mixpanelDeliveryException = e;
    }

    for (AnalyticsDeliveryExecution<AnalyticsEvent> event : eventInBatch) {
      if (mixpanelDeliveryException != null) {
        event.fatalError(mixpanelDeliveryException);
        continue;
      }
      event.delivered();
    }
  }

}
