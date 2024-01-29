package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The event. They are the things that happen in our product.  Every event type will extend this object and add its properties.  We try to follow the element of a sequential diagram with the following participants: * user: the user * agent: the agent that the user is using * request: the request * time: the state times * channel: the channel properties (how the user came in)  All properties that are tied only to the event type/name are properties added at the root with a primary datatype.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEvent   {


  protected String guid;

  protected String typeName;

  protected String typeGuid;

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
  * @return guid A global unique identifier for each event.  It allows to delete duplicate.  It's a timestamp based UUID where the timestamp is the value of the creation time. (It permits to quickly retrieve via partition an event)  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid A global unique identifier for each event.  It allows to delete duplicate.  It's a timestamp based UUID where the timestamp is the value of the creation time. (It permits to quickly retrieve via partition an event)  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return typeName The type name (known also as event name) An human readable name that will be normalized to camel case with space separator
  */
  @JsonProperty("typeName")
  public String getTypeName() {
    return typeName;
  }

  /**
  * @param typeName The type name (known also as event name) An human readable name that will be normalized to camel case with space separator
  */
  @SuppressWarnings("unused")
  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  /**
  * @return typeGuid A immutable type guid that permit to change the name For now: 0 for sign-up 1 for sign-in 2 for user profile update
  */
  @JsonProperty("typeGuid")
  public String getTypeGuid() {
    return typeGuid;
  }

  /**
  * @param typeGuid A immutable type guid that permit to change the name For now: 0 for sign-up 1 for sign-in 2 for user profile update
  */
  @SuppressWarnings("unused")
  public void setTypeGuid(String typeGuid) {
    this.typeGuid = typeGuid;
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

            Objects.equals(guid, analyticsEvent.guid) && Objects.equals(typeName, analyticsEvent.typeName) && Objects.equals(typeGuid, analyticsEvent.typeGuid) && Objects.equals(state, analyticsEvent.state) && Objects.equals(app, analyticsEvent.app) && Objects.equals(user, analyticsEvent.user) && Objects.equals(request, analyticsEvent.request) && Objects.equals(utm, analyticsEvent.utm) && Objects.equals(attr, analyticsEvent.attr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, typeName, typeGuid, state, app, user, request, utm, attr);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
