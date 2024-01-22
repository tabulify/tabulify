package net.bytle.vertx.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represent a server event with a name and its properties
 */
public abstract class AnalyticsServerEvent {

  /**
   * The process that created this event
   */
  private Integer flowId;
  /**
   * The app that created this event
   */
  private String appId;
  /**
   * The realm of the app
   */
  private String appRealmId;
  /**
   * The organization of the app
   */
  private String appOrganizationId;

  /**
   * @return the name/type of the event
   */
  public abstract String getName();

  public String getAppId(){
    return this.appId;
  }

  public String getAppRealmId(){
    return this.appRealmId;
  }

  public String getAppOrganizationId(){
    return this.appOrganizationId;
  }

  public void setAppId(String appId){
    this.appId = appId;
  }

  public void setAppRealmId(String appRealmId){
    this.appRealmId = appRealmId;
  }

  public void setAppOrganizationId(String appOrganizationId){
    this.appOrganizationId = appOrganizationId;
  }


  /**
   * The flow of sign-up
   */
  @JsonProperty("flowId")
  public Integer getFlowId() {

    return this.flowId;

  }

  public void setFlowId(Integer flowId) {
    this.flowId = flowId;
  }

}
