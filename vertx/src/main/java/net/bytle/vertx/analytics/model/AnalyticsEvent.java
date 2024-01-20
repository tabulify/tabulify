package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The event. They are the things that happen in our product.  We follow a mix of * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;Segment Spec&lt;/a&gt; * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;MixPanel Spec&lt;/a&gt;  Every event type will extend this object and add its properties.  We try to follow the element of a sequential diagram with the following participants: * user: the user * agent: the agent that the user is using * request: the request * time: the state times * channel: the channel properties (how the user came in)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEvent   {


  protected String id;

  protected String name;

  protected AnalyticsEventTime time;

  protected AnalyticsEventApp app;

  protected AnalyticsEventUser user;

  protected AnalyticsEventRequest request;

  protected AnalyticsEventChannel channel;

  protected Map<String, Object> properties = new HashMap<>();

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEvent () {
  }

  /**
  * @return id A unique identifier for each event.  It allows to delete duplicate.  It's a timestamp based UUID where the timestamp is the value of the creation time. (It permits to quickly retrieve via partition an event)  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
  * @param id A unique identifier for each event.  It allows to delete duplicate.  It's a timestamp based UUID where the timestamp is the value of the creation time. (It permits to quickly retrieve via partition an event)  It's known for: * segment as messageId * mixpanel as $insert_id
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
  * @return time
  */
  @JsonProperty("time")
  public AnalyticsEventTime getTime() {
    return time;
  }

  /**
  * @param time Set time
  */
  @SuppressWarnings("unused")
  public void setTime(AnalyticsEventTime time) {
    this.time = time;
  }

  /**
  * @return app
  */
  @JsonProperty("app")
  public AnalyticsEventApp getApp() {
    return app;
  }

  /**
  * @param app Set app
  */
  @SuppressWarnings("unused")
  public void setApp(AnalyticsEventApp app) {
    this.app = app;
  }

  /**
  * @return user
  */
  @JsonProperty("user")
  public AnalyticsEventUser getUser() {
    return user;
  }

  /**
  * @param user Set user
  */
  @SuppressWarnings("unused")
  public void setUser(AnalyticsEventUser user) {
    this.user = user;
  }

  /**
  * @return request
  */
  @JsonProperty("request")
  public AnalyticsEventRequest getRequest() {
    return request;
  }

  /**
  * @param request Set request
  */
  @SuppressWarnings("unused")
  public void setRequest(AnalyticsEventRequest request) {
    this.request = request;
  }

  /**
  * @return channel
  */
  @JsonProperty("channel")
  public AnalyticsEventChannel getChannel() {
    return channel;
  }

  /**
  * @param channel Set channel
  */
  @SuppressWarnings("unused")
  public void setChannel(AnalyticsEventChannel channel) {
    this.channel = channel;
  }

  /**
  * @return properties The additional unknown event properties
  */
  @JsonProperty("properties")
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
  * @param properties The additional unknown event properties
  */
  @SuppressWarnings("unused")
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
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
    return

            Objects.equals(id, analyticsEvent.id) && Objects.equals(name, analyticsEvent.name) && Objects.equals(time, analyticsEvent.time) && Objects.equals(app, analyticsEvent.app) && Objects.equals(user, analyticsEvent.user) && Objects.equals(request, analyticsEvent.request) && Objects.equals(channel, analyticsEvent.channel) && Objects.equals(properties, analyticsEvent.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, time, app, user, request, channel, properties);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
