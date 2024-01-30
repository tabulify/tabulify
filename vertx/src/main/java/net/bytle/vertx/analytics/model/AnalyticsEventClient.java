package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The properties of the client that created this event
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventClient   {


  protected String clientGuid;

  protected String appGuid;

  protected String appHandle;

  protected String appRealmGuid;

  protected String appRealmHandle;

  protected String appOrganisationGuid;

  protected String appOrganisationHandle;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventClient () {
  }

  /**
  * @return clientGuid The client id that has created this event (never change)
  */
  @JsonProperty("clientGuid")
  public String getClientGuid() {
    return clientGuid;
  }

  /**
  * @param clientGuid The client id that has created this event (never change)
  */
  @SuppressWarnings("unused")
  public void setClientGuid(String clientGuid) {
    this.clientGuid = clientGuid;
  }

  /**
  * @return appGuid The app id that has created this event (never change)
  */
  @JsonProperty("appGuid")
  public String getAppGuid() {
    return appGuid;
  }

  /**
  * @param appGuid The app id that has created this event (never change)
  */
  @SuppressWarnings("unused")
  public void setAppGuid(String appGuid) {
    this.appGuid = appGuid;
  }

  /**
  * @return appHandle The app name identifier (human identifier, may change)
  */
  @JsonProperty("appHandle")
  public String getAppHandle() {
    return appHandle;
  }

  /**
  * @param appHandle The app name identifier (human identifier, may change)
  */
  @SuppressWarnings("unused")
  public void setAppHandle(String appHandle) {
    this.appHandle = appHandle;
  }

  /**
  * @return appRealmGuid The realm of the app If the user is not anonymous, it is the user audience.
  */
  @JsonProperty("appRealmGuid")
  public String getAppRealmGuid() {
    return appRealmGuid;
  }

  /**
  * @param appRealmGuid The realm of the app If the user is not anonymous, it is the user audience.
  */
  @SuppressWarnings("unused")
  public void setAppRealmGuid(String appRealmGuid) {
    this.appRealmGuid = appRealmGuid;
  }

  /**
  * @return appRealmHandle The realm human identifier (may change)
  */
  @JsonProperty("appRealmHandle")
  public String getAppRealmHandle() {
    return appRealmHandle;
  }

  /**
  * @param appRealmHandle The realm human identifier (may change)
  */
  @SuppressWarnings("unused")
  public void setAppRealmHandle(String appRealmHandle) {
    this.appRealmHandle = appRealmHandle;
  }

  /**
  * @return appOrganisationGuid the organisation id (the billing logical unit, known also as the group id)
  */
  @JsonProperty("appOrganisationGuid")
  public String getAppOrganisationGuid() {
    return appOrganisationGuid;
  }

  /**
  * @param appOrganisationGuid the organisation id (the billing logical unit, known also as the group id)
  */
  @SuppressWarnings("unused")
  public void setAppOrganisationGuid(String appOrganisationGuid) {
    this.appOrganisationGuid = appOrganisationGuid;
  }

  /**
  * @return appOrganisationHandle the organisation handle (human identifier, may change)
  */
  @JsonProperty("appOrganisationHandle")
  public String getAppOrganisationHandle() {
    return appOrganisationHandle;
  }

  /**
  * @param appOrganisationHandle the organisation handle (human identifier, may change)
  */
  @SuppressWarnings("unused")
  public void setAppOrganisationHandle(String appOrganisationHandle) {
    this.appOrganisationHandle = appOrganisationHandle;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventClient analyticsEventClient = (AnalyticsEventClient) o;
    return

            Objects.equals(clientGuid, analyticsEventClient.clientGuid) && Objects.equals(appGuid, analyticsEventClient.appGuid) && Objects.equals(appHandle, analyticsEventClient.appHandle) && Objects.equals(appRealmGuid, analyticsEventClient.appRealmGuid) && Objects.equals(appRealmHandle, analyticsEventClient.appRealmHandle) && Objects.equals(appOrganisationGuid, analyticsEventClient.appOrganisationGuid) && Objects.equals(appOrganisationHandle, analyticsEventClient.appOrganisationHandle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientGuid, appGuid, appHandle, appRealmGuid, appRealmHandle, appOrganisationGuid, appOrganisationHandle);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
