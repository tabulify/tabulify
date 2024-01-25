package net.bytle.vertx.analytics.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.vertx.analytics.model.AnalyticsEventApp;
import net.bytle.vertx.analytics.model.AnalyticsEventRequest;

/**
 * Represent a server event with a name and its properties
 */
public abstract class AnalyticsServerEvent {


  private AnalyticsEventRequest request;

  public AnalyticsServerEvent() {
    /**
     * Facility to not recreate the object
     */
    this.app = new AnalyticsEventApp();
    this.request = new AnalyticsEventRequest();
  }


  /**
   * The app information
   */
  protected AnalyticsEventApp app;

  /**
   * @return the name/type of the event
   * JsonIgnore to indicate that is not a custom event attribute
   * The event builder take care of taking this build-in attributes
   */
  @JsonIgnore()
  public abstract String getName();

  /**
   * @return app
   * JsonIgnore to indicate that is not a custom event attribute
   * The event builder take care of taking this build-in attributes
   */
  @JsonIgnore()
  @JsonProperty("request")
  public AnalyticsEventRequest getRequest() {
    return request;
  }

  /**
   * @return app
   * JsonIgnore to indicate that is not a custom event attribute
   * The event builder take care of taking this build-in attributes
   */
  @JsonIgnore()
  @JsonProperty("app")
  public AnalyticsEventApp getApp() {
    return app;
  }


}
