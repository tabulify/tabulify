package net.bytle.vertx;

import io.vertx.core.json.JsonObject;
import jakarta.mail.internet.AddressException;
import net.bytle.exception.NotFoundException;
import net.bytle.type.time.Timestamp;

import java.net.URI;
import java.util.Map;

/**
 * Mixpanel utility class
 * based on:
 * <a href="https://github.com/mixpanel/mixpanel-java/blob/master/src/demo/java/com/mixpanel/mixpanelapi/demo/MixpanelAPIDemo.java"></a>
 */
public class AnalyticsMixPanel {



  /**
   * @param user - the user
   * @param ip   - the ip when the user was created for geo-localization
   * @return the json mixpanel object
   */
  public static JsonObject toMixPanelUser(AnalyticsUser user, String ip) {
    JsonObject props = new JsonObject();
    props.put("$distinct_id", user.getId());
    // $group_id, the group identifier, for group profiles, as these are the canonical identifiers in Mixpanel.
    props.put("$email", user.getEmail());
    try {
      props.put("$name", user.getNameOrEmail());
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
   *
   * @param event the event
   * @return the JSON for Mixpanel without the user id (ie distinct id) because it's mandatory to add it in the event function signature of Mixpanel
   */
  protected static JsonObject toMixpanelPropsWithoutUserId(AnalyticsEvent event) {

    JsonObject props = new JsonObject();

    /**
     * $device_id: The anonymous / device id
     */
    props.put("$device_id", event.getDeviceId());

    /**
     * $user_id
     */
    AnalyticsUser user = event.getUser();
    if (user != null) {
      props.put("$user_id", event.getUser().getId());
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
    String ip = event.getContext().getIp();
    if (ip != null) {
      props.put("ip", ip);
    }

    /**
     * $os: OS of the event sender.
     */
    AnalyticsOperatingSystem os = event.getContext().getOs();
    props.put("$os", os.getName());

    /**
     * Session
     * <a href="https://docs.mixpanel.com/docs/analysis/advanced/sessions'>Session</a>
     */

    /**
     * Channel
     */
    AnalyticsEventChannel channel = event.getContext().getChannel();
    if (channel != null) {
      props.put("channel", channel.toString());
    }

    /**
     * Timestamp
     */
    props.put("creationTime", Timestamp.createFromLocalDateTime(event.getCreationTime()).toIsoString());
//    props.put("sendingTime", Timestamp.createFromLocalDateTime(event.getSendingTime()).toIsoString());


    /**
     * Additional properties along with events
     */
    for(Map.Entry<String, Object> entry: event.getProperties().entrySet()){
      /**
       * Does the `toString` work with MixPanel Data???
       * https://docs.mixpanel.com/docs/other-bits/tutorials/developers/mixpanel-for-developers-fundamentals#supported-data-types
       */
      props.put(entry.getKey(), entry.getValue().toString());
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
  protected static String toMixPanelUserDistinctId(AnalyticsEvent event) {
    AnalyticsUser user = event.getUser();
    if (user != null) {
      return user.getId();
    }
    return event.getDeviceId();
  }
}
