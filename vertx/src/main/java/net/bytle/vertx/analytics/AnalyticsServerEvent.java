package net.bytle.vertx.analytics;

/**
 * Represent a server event with a name and its properties
 */
public abstract class AnalyticsServerEvent {


  private String appId;
  private String appRealmId;
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



}
