package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The event. They are the things that happen in our product.  We follow a mix of * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;Segment Spec&lt;/a&gt; * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;MixPanel Spec&lt;/a&gt;  Every event type will extend this object and add its properties.  We try to follow the element of a sequential diagram with the following participants: * user: the user * agent: the agent that the user is using * request: the request * time: the state times * channel: the channel properties (how the user came in)  All properties that are tied only to the event type/name are properties added at the root with a primary datatype.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEvent   {


  protected String id;

  protected String name;

  protected AnalyticsEventState state;

  protected AnalyticsEventApp app;

  protected AnalyticsEventUser user;

  protected AnalyticsEventRequest request;

  protected AnalyticsEventUtm utm;

  protected Map<String, Object> attr = new HashMap<>();

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
  * @return name The event name (known also as type) An human readable name that will be normalized
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The event name (known also as type) An human readable name that will be normalized
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return state
  */
  @JsonProperty("state")
  public AnalyticsEventState getState() {
    return state;
  }

  /**
  * @param state Set state
  */
  @SuppressWarnings("unused")
  public void setState(AnalyticsEventState state) {
    this.state = state;
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
  * @return utm
  */
  @JsonProperty("utm")
  public AnalyticsEventUtm getUtm() {
    return utm;
  }

  /**
  * @param utm Set utm
  */
  @SuppressWarnings("unused")
  public void setUtm(AnalyticsEventUtm utm) {
    this.utm = utm;
  }

  /**
  * @return attr The custom attributes for the event type
  */
  @JsonProperty("attr")
  public Map<String, Object> getAttr() {
    return attr;
  }

  /**
  * @param attr The custom attributes for the event type
  */
  @SuppressWarnings("unused")
  public void setAttr(Map<String, Object> attr) {
    this.attr = attr;
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

            Objects.equals(id, analyticsEvent.id) && Objects.equals(name, analyticsEvent.name) && Objects.equals(state, analyticsEvent.state) && Objects.equals(app, analyticsEvent.app) && Objects.equals(user, analyticsEvent.user) && Objects.equals(request, analyticsEvent.request) && Objects.equals(utm, analyticsEvent.utm) && Objects.equals(attr, analyticsEvent.attr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, state, app, user, request, utm, attr);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
