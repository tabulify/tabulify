package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The event. They are the things that happen in our product.  We follow a mix of * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;Segment Spec&lt;/a&gt; * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;MixPanel Spec&lt;/a&gt;  The segment spec separates the event property from the context as we do.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEvent   {

  private String id;
  private String name;
  private Map<String, Object> properties = new HashMap<>();
  private String deviceId;
  private User user;
  private AnalyticsEventContext context;
  private LocalDateTime receptionTime;
  private LocalDateTime sendingTime;
  private LocalDateTime creationTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEvent () {
  }

  /**
  * @return id A unique identifier for each event.  It allows to delete duplicate.  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
  * @param id A unique identifier for each event.  It allows to delete duplicate.  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @SuppressWarnings("unused")
  public void setId(String id) {
    this.id = id;
  }

  /**
  * @return name the event name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name the event name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return properties The additional event properties
  */
  @JsonProperty("properties")
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
  * @param properties The additional event properties
  */
  @SuppressWarnings("unused")
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  /**
  * @return deviceId The device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @JsonProperty("deviceId")
  public String getDeviceId() {
    return deviceId;
  }

  /**
  * @param deviceId The device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @SuppressWarnings("unused")
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
  * @return user
  */
  @JsonProperty("user")
  public User getUser() {
    return user;
  }

  /**
  * @param user Set user
  */
  @SuppressWarnings("unused")
  public void setUser(User user) {
    this.user = user;
  }

  /**
  * @return context
  */
  @JsonProperty("context")
  public AnalyticsEventContext getContext() {
    return context;
  }

  /**
  * @param context Set context
  */
  @SuppressWarnings("unused")
  public void setContext(AnalyticsEventContext context) {
    this.context = context;
  }

  /**
  * @return receptionTime The timestamp of when a message was received (if created and send by a client)
  */
  @JsonProperty("receptionTime")
  public LocalDateTime getReceptionTime() {
    return receptionTime;
  }

  /**
  * @param receptionTime The timestamp of when a message was received (if created and send by a client)
  */
  @SuppressWarnings("unused")
  public void setReceptionTime(LocalDateTime receptionTime) {
    this.receptionTime = receptionTime;
  }

  /**
  * @return sendingTime The timestamp of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @JsonProperty("sendingTime")
  public LocalDateTime getSendingTime() {
    return sendingTime;
  }

  /**
  * @param sendingTime The timestamp of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @SuppressWarnings("unused")
  public void setSendingTime(LocalDateTime sendingTime) {
    this.sendingTime = sendingTime;
  }

  /**
  * @return creationTime The timestamp when the object was created
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The timestamp when the object was created
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEvent analyticsEvent = (AnalyticsEvent) o;
    return Objects.equals(id, analyticsEvent.id) &&
        Objects.equals(name, analyticsEvent.name) &&
        Objects.equals(properties, analyticsEvent.properties) &&
        Objects.equals(deviceId, analyticsEvent.deviceId) &&
        Objects.equals(user, analyticsEvent.user) &&
        Objects.equals(context, analyticsEvent.context) &&
        Objects.equals(receptionTime, analyticsEvent.receptionTime) &&
        Objects.equals(sendingTime, analyticsEvent.sendingTime) &&
        Objects.equals(creationTime, analyticsEvent.creationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, properties, deviceId, user, context, receptionTime, sendingTime, creationTime);
  }

  @Override
  public String toString() {
    return "class AnalyticsEvent {\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
