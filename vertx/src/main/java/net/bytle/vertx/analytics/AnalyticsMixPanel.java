package net.bytle.vertx.analytics;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import io.vertx.core.json.JsonObject;
import jakarta.mail.internet.AddressException;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNameNormalizer;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.Server;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsUser;
import net.bytle.vertx.auth.AuthUserUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

/**
 * MixPanel - modify the time based on the project time zone!
 * Be sure to have UTC
 * <p>
 * Mixpanel utility class
 * based on:
 * <a href="https://github.com/mixpanel/mixpanel-java/blob/master/src/demo/java/com/mixpanel/mixpanelapi/demo/MixpanelAPIDemo.java"></a>
 */
public class AnalyticsMixPanel {

  private static final String MIX_PANEL_PROJECT_TOKEN = "eraldy.mixpanel.project.token";
  private static final String MIXPANEL_KEY_CASE_CONF = "mixpanel.key.case";
  private final KeyNameNormalizer.WordCase keyCase;
  static Logger LOGGER = LogManager.getLogger(AnalyticsMixPanel.class);

  private final MixpanelAPI mixpanel;
  private final MessageBuilder messageBuilder;
  public AnalyticsMixPanel(Server server) throws ConfigIllegalException {
    String keyCase = server.getConfigAccessor().getString(MIXPANEL_KEY_CASE_CONF,KeyNameNormalizer.WordCase.SNAKE.toString());
      KeyNameNormalizer.WordCase wordCase;
      try {
          wordCase = Casts.cast(keyCase, KeyNameNormalizer.WordCase.class);
      } catch (CastException e) {
          throw new ConfigIllegalException("The value ("+keyCase+") from the configuration ("+MIXPANEL_KEY_CASE_CONF+") is not valid. The possibles values are: "+ Enums.toConstantAsStringCommaSeparated(KeyNameNormalizer.WordCase.class),e);
      }
      this.keyCase = wordCase;
    String projectToken = server.getConfigAccessor().getString(MIX_PANEL_PROJECT_TOKEN);
    if (projectToken == null) {
      throw new ConfigIllegalException("MixPanelTracker: A project token is mandatory to send the event. Add one in the conf file with the attribute (" + MIX_PANEL_PROJECT_TOKEN + ")");
    }
    this.messageBuilder = new MessageBuilder(projectToken);

    // Use an instance of MixpanelAPI to send the messages
    // to Mixpanel's servers.
    this.mixpanel = new MixpanelAPI();
  }

  /**
   * @param user - the user
   * @param ip   - the ip when the user was created for geo-localization
   * @return the json mixpanel object
   */
  public JsonObject toMixPanelUser(AnalyticsUser user, String ip) {
    JsonObject props = new JsonObject();
    props.put("$distinct_id", user.getId());
    // $group_id, the group identifier, for group profiles, as these are the canonical identifiers in Mixpanel.
    props.put("$email", user.getEmail());
    try {
      props.put("$name", AuthUserUtils.getNameOrNameFromEmail(user.getGivenName(), user.getEmail()));
    } catch (NotFoundException | AddressException e) {
      // should not
    }
    URI avatar = user.getAvatar();
    if (avatar != null) {
      props.put("$avatar", avatar.toString());
    }
    props.put("$created", user.getCreationTime().toString());
    /**
     * ip, determine $city, $region, $country_code and $timezone
     */
    if (ip != null) {
      props.put("ip", ip);
    }
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

    String agentId = event.getRequest().getAgentId();
    if (agentId != null) {
      props.put("$device_id", agentId);
    }

    /**
     * $user_id
     */
    String userId = event.getUser().getUserId();
    if (userId != null) {
      props.put("$user_id", userId);
    }
    String userEmail = event.getUser().getUserEmail();
    if (userEmail != null) {
      props.put("user_email", userEmail);
    }

    /**
     * $insert_id: A unique identifier for the event,
     * used to deduplicate events that are accidentally sent multiple times.
     */
    props.put("$insert_id", event.getId());

    /**
     * IP
     * Geolocation is by default turned on
     * https://docs.mixpanel.com/docs/tracking/how-tos/privacy-friendly-tracking#disabling-geolocation
     */
    String ip = event.getRequest().getRemoteIp();
    if (ip != null) {
      props.put("ip", ip);
    }

    /**
     * Request
     * Session Properties
     * <a href="https://docs.mixpanel.com/docs/features/sessions#session-properties'>Session</a>
     */
    String sessionId = event.getRequest().getSessionId();
    if (sessionId != null) {
      props.put("session_id", sessionId);
    }
    URI originUri = event.getRequest().getOriginUri();
    if (originUri != null) {
      try {
        props.put("$current_url", originUri.toURL().toString());
      } catch (MalformedURLException e) {
        // not an url
      }
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
     * We have discovered that ultimately they store all date in Epoch Sec (a downalod give you epoch data)
     * Setting it as Epoch, it just works.
     */
    Long creationTimeIso = Timestamp.createFromLocalDateTime(event.getState().getCreationTime()).toEpochSec();
    props.put("$time", creationTimeIso);

    /**
     * Group Analytics is an add-on
     * We use for now custom properties
     * https://docs.mixpanel.com/docs/tracking-methods/sdks/java#group-analytics
     * https://docs.mixpanel.com/docs/data-structure/advanced/group-analytics
     */
    String appId = event.getApp().getAppId();
    props.put("app_id", appId);
    props.put("app_realm_id", event.getApp().getAppRealmId());
    props.put("app_organization_id", event.getApp().getAppOrganisationId());

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
          LOGGER.error("The value of the key (" + entry.getKey() + ") for the event (" + event.getName() + ") is null. The value was ignored.");
        }
        continue;
      }
      String valueString = value.toString();
      if (valueString.isBlank()) {
        continue;
      }
      String snakeCaseKey = KeyNameNormalizer.createFromString(entry.getKey()).toWordCase(keyCase);
      props.put(snakeCaseKey, valueString);
    }
    return props;

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
    String userId = event.getUser().getUserId();
    if (userId != null) {
      return userId;
    }
    return event.getApp().getAppId();
  }



  public JSONObject buildEvent(AnalyticsEvent event) {
    JsonObject vertxJsonObject = this.toMixpanelPropsWithoutUserId(event);
    JSONObject mixPanelJsonObject = new JSONObject(vertxJsonObject.getMap());
    String mixPanelUserDistinctId = this.toMixPanelUserDistinctId(event);
    String name = event.getName();
    return this.messageBuilder.event(
      mixPanelUserDistinctId,
      name,
      mixPanelJsonObject
    );
  }

  public void deliver(ClientDelivery delivery) throws IOException {
    this.mixpanel.deliver(delivery);
  }
}
